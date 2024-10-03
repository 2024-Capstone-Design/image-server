package com.dingdong.imageserver.service.firebase;

import com.dingdong.imageserver.constant.FirebaseFieldConstants;
import com.dingdong.imageserver.dto.request.ReImagineRequestDTO;
import com.dingdong.imageserver.dto.service.CommonImageGenerationDTO;
import com.google.firebase.database.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 파이어베이스 데이터 업데이트
 */
@Service
@Slf4j
public class FirebaseUpdateService extends FirebaseBaseService {


    public FirebaseUpdateService(FirebaseDatabase firebaseDatabase) {
        super(firebaseDatabase);
    }

    /**
     * 이미지 생성 전 studentTaskId로 해당 작업 초기화
     * @param studentTaskId
     * @param callback
     */
    public void initializeTaskInFirebase(Long studentTaskId, Consumer<Boolean> callback) {
        DatabaseReference taskRef = getDatabaseReference(String.valueOf(studentTaskId));

        // complete 값 확인
        taskRef.child(FirebaseFieldConstants.COMPLETED_FIELD).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Boolean isComplete = dataSnapshot.getValue(Boolean.class);

                if (dataSnapshot.exists() && Boolean.FALSE.equals(isComplete)) {
                    log.warn("이미 진행중인 Task 입니다.");
                    callback.accept(false);
                    return;
                }

                Map<String, Object> taskData = createTaskInitializationData();
                taskRef.setValueAsync(taskData);
                callback.accept(true);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.accept(false);
            }
        });
    }

    // 작업 초기화 데이터를 생성하는 메소드
    private Map<String, Object> createTaskInitializationData() {
        Map<String, Object> taskData = new HashMap<>();
        taskData.put(FirebaseFieldConstants.COMPLETED_FIELD, false);
        taskData.put(FirebaseFieldConstants.ERROR_FIELD, false);
        taskData.put(FirebaseFieldConstants.START_TIME_FIELD, LocalDateTime.now().toString());
        taskData.put(FirebaseFieldConstants.END_TIME_FIELD, null);
        taskData.put(FirebaseFieldConstants.CHARACTER_FIELD, null);
        taskData.put(FirebaseFieldConstants.BACKGROUND_FIELD, null);

        return taskData;
    }

    /**
     * 이미지 생성 프로세스 완료 후 상태 업데이트
     * @param studentTaskId
     */
    public void finalizeTask(long studentTaskId) {
        DatabaseReference taskRef = getDatabaseReference(String.valueOf(studentTaskId));
        Map<String, Object> updateData = Map.of(
                FirebaseFieldConstants.END_TIME_FIELD, LocalDateTime.now().toString(),
                FirebaseFieldConstants.COMPLETED_FIELD, true
        );
        taskRef.updateChildrenAsync(updateData);
    }

    /**
     * 이미지 재생성 시 기존 이미지 삭제 후 재시도하기
     * @param requestDTO
     */
    public void clearCharacterReferenceImage(ReImagineRequestDTO requestDTO) {
        DatabaseReference taskRef = getDatabaseReference(String.valueOf(requestDTO.getStudentTaskId()));

        HashMap<String, Object> taskUpdates = new HashMap<>();
        taskUpdates.put(FirebaseFieldConstants.COMPLETED_FIELD, false);
        taskUpdates.put(FirebaseFieldConstants.ERROR_FIELD, false);
        taskUpdates.put(FirebaseFieldConstants.START_TIME_FIELD, LocalDateTime.now().toString());
        taskUpdates.put(FirebaseFieldConstants.END_TIME_FIELD, null);
        taskUpdates.put(FirebaseFieldConstants.REGENERATE_FIELD, true);

        updateFirebaseData(taskRef, taskUpdates);

        DatabaseReference ref = getDatabaseReference(
                String.valueOf(requestDTO.getStudentTaskId()),
                requestDTO.getPromptType().name(),
                requestDTO.getImageId()
        );

        ref.removeValue((databaseError, databaseReference) -> {
            if (databaseError != null) {
                log.error("Error removing image ID: {}", databaseError.getMessage());
            } else {
                log.info("Image ID successfully removed for studentTaskId: {}", requestDTO.getStudentTaskId());
            }
        });
    }


    // 프롬프트 상태 업데이트 메소드 (IMAGINE 시)
    public String updatePromptStatus(String studentTaskId, CommonImageGenerationDTO promptDTO, String prompt, String status) {

        DatabaseReference characterRef = getDatabaseReference(studentTaskId, promptDTO.getPromptType().name()).push();
        String generatedId = characterRef.getKey();

        Map<String, Object> updates = createPromptUpdateMap(promptDTO.getName(), prompt, status);
        updateFirebaseData(characterRef, updates);

        return generatedId;
    }

    // ID로 프롬프트 상태 업데이트 메소드 (UPSCALE 시)
    public void updateErrorStatusById(String studentTaskId, String imageId, CommonImageGenerationDTO promptDTO, String status, String errorMessage) {

        DatabaseReference characterRef = getDatabaseReference(studentTaskId, promptDTO.getPromptType().name(), imageId);

        Map<String, Object> updates = createStatusUpdateMap(status, errorMessage);
        updateFirebaseData(characterRef, updates);

        if (errorMessage != null) {
            System.out.println("errorMessage :: " + errorMessage);
            updateTaskErrorStatus(studentTaskId, errorMessage);
        }
    }

    // 작업 에러 상태 업데이트 메소드
    public void updateTaskErrorStatus(String studentTaskId, String errorMessage) {
        DatabaseReference taskRef = getDatabaseReference(studentTaskId);
        taskRef.updateChildrenAsync(Map.of(FirebaseFieldConstants.ERROR_FIELD, true, FirebaseFieldConstants.ERROR_MESSAGE_FIELD, errorMessage));
    }

    // 공통 업데이트 맵 생성 (프롬프트 상태 업데이트)
    private Map<String, Object> createPromptUpdateMap(String name, String prompt, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(FirebaseFieldConstants.NAME_FIELD, name);
        updates.put(FirebaseFieldConstants.PROMPT_FIELD, prompt);
        updates.put(FirebaseFieldConstants.STATUS_FIELD, status);
        updates.put(FirebaseFieldConstants.START_TIME_FIELD, LocalDateTime.now().toString());

        return updates;
    }

    // 상태 업데이트 맵 생성 (에러 메시지 포함)
    private Map<String, Object> createStatusUpdateMap(String status, String errorMessage) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(FirebaseFieldConstants.STATUS_FIELD, status);
        updates.put(FirebaseFieldConstants.ERROR_FIELD, true);

        if (errorMessage != null) {
            updates.put(FirebaseFieldConstants.ERROR_MESSAGE_FIELD, errorMessage);
        }

        return updates;
    }

    public DatabaseReference getCharacterReference(String studentTaskId, String imageId, CommonImageGenerationDTO character) {
        return getDatabaseReference(studentTaskId, character.getPromptType().name(), imageId);
    }
}

