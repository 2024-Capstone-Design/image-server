package com.dingdong.imageserver.service.firebase;

import com.dingdong.imageserver.constant.FirebaseFieldConstants;
import com.dingdong.imageserver.dto.ReImagineRequestDTO;
import com.dingdong.imageserver.dto.prompt.CommonPromptDTO;
import com.dingdong.imageserver.model.DataCallback;
import com.google.firebase.database.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FirebasePromptService extends FirebaseBaseService {

    public FirebasePromptService(FirebaseDatabase firebaseDatabase) {
        super(firebaseDatabase);
    }

    // 프롬프트 상태 업데이트 메소드 (IMAGINE 시)
    public String updatePromptStatus(String studentTaskId, CommonPromptDTO promptDTO, String prompt, String status, String errorMessage) {
        DatabaseReference characterRef = getDatabaseReference(studentTaskId, promptDTO.getPromptType().name()).push();
        String generatedId = characterRef.getKey();

        Map<String, Object> updates = createPromptUpdateMap(promptDTO.getName(), prompt, status, errorMessage);
        updateFirebaseData(characterRef, updates);

        if (errorMessage != null) {
            updateTaskErrorStatus(studentTaskId, true);
        }

        return generatedId;
    }

    // ID로 프롬프트 상태 업데이트 메소드 (UPSCALE 시)
    public void updatePromptStatusById(String studentTaskId, String generatedId, CommonPromptDTO promptDTO, String status, String errorMessage) {
        DatabaseReference characterRef = getDatabaseReference(studentTaskId, promptDTO.getPromptType().name(), generatedId);

        Map<String, Object> updates = createStatusUpdateMap(status, errorMessage);
        updateFirebaseData(characterRef, updates);

        if (errorMessage != null) {
            updateTaskErrorStatus(studentTaskId, true);
        }
    }

    // 캐릭터 이미지 URL 업데이트 메소드
    public void updateCharacterImageUrls(String studentTaskId, String generatedId, CommonPromptDTO promptDTO, List<String> imageUrls) {
        DatabaseReference characterRef = getDatabaseReference(studentTaskId, promptDTO.getPromptType().name(), generatedId);

        Map<String, Object> updates = Map.of(
                FirebaseFieldConstants.IMAGE_URL_FIELD, imageUrls,
                FirebaseFieldConstants.STATUS_FIELD, "complete"
        );

        updateFirebaseData(characterRef, updates);
    }

    // ID로 프롬프트 조회 메소드
    public void getPromptById(ReImagineRequestDTO requestDTO, DataCallback callback) {
        String studentTaskId = String.valueOf(requestDTO.getStudentTaskId());
        String imageId = String.valueOf(requestDTO.getImageId());

        DatabaseReference characterRef = getDatabaseReference(studentTaskId, requestDTO.getPromptType().name(), imageId);

        characterRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String prompt = dataSnapshot.child(FirebaseFieldConstants.PROMPT_FIELD).getValue(String.class);
                    String name = dataSnapshot.child(FirebaseFieldConstants.NAME_FIELD).getValue(String.class);
                    if (prompt != null && name != null) {
                        callback.onSuccess(prompt, name);
                    } else {
                        callback.onFailure("Prompt not found");
                    }
                } else {
                    callback.onFailure("Data not found");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onFailure(databaseError.getMessage());
            }
        });
    }

    // 작업 에러 상태 업데이트 메소드
    public void updateTaskErrorStatus(String studentTaskId, boolean isError) {
        DatabaseReference taskRef = getDatabaseReference(studentTaskId);
        taskRef.updateChildrenAsync(Map.of(FirebaseFieldConstants.ERROR_FIELD, isError));
    }

    // 공통 업데이트 맵 생성 (프롬프트 상태 업데이트)
    private Map<String, Object> createPromptUpdateMap(String name, String prompt, String status, String errorMessage) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(FirebaseFieldConstants.NAME_FIELD, name);
        updates.put(FirebaseFieldConstants.PROMPT_FIELD, prompt);
        updates.put(FirebaseFieldConstants.STATUS_FIELD, status);

        if (errorMessage != null) {
            updates.put(FirebaseFieldConstants.ERROR_MESSAGE_FIELD, errorMessage);
        }

        return updates;
    }

    // 상태 업데이트 맵 생성 (에러 메시지 포함)
    private Map<String, Object> createStatusUpdateMap(String status, String errorMessage) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(FirebaseFieldConstants.STATUS_FIELD, status);

        if (errorMessage != null) {
            updates.put(FirebaseFieldConstants.ERROR_MESSAGE_FIELD, errorMessage);
        }

        return updates;
    }
}

