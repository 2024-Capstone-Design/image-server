package com.dingdong.imageserver.service.firebase;

import com.dingdong.imageserver.constant.FirebaseFieldConstants;
import com.dingdong.imageserver.dto.ReImagineRequestDTO;
import com.dingdong.imageserver.dto.prompt.CommonPromptDTO;
import com.dingdong.imageserver.model.DataCallback;
import com.dingdong.imageserver.model.FairytaleImage;
import com.dingdong.imageserver.model.FairytaleImageRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FirebasePromptService extends FirebaseBaseService {

    private final FairytaleImageRepository fairytaleImageRepository;


    public FirebasePromptService(FirebaseDatabase firebaseDatabase, FairytaleImageRepository fairytaleImageRepository) {
        super(firebaseDatabase);
        this.fairytaleImageRepository = fairytaleImageRepository;
    }

    // 프롬프트 상태 업데이트 메소드 (IMAGINE 시)
    public String updatePromptStatus(String studentTaskId, Long fairytaleId, CommonPromptDTO promptDTO, String prompt, String status, String errorMessage) {
        DatabaseReference characterRef = getDatabaseReference(studentTaskId, promptDTO.getPromptType().name()).push();
        String generatedId = characterRef.getKey();

        Map<String, Object> updates = createPromptUpdateMap(promptDTO.getName(), prompt, status, errorMessage);
        updateFirebaseData(characterRef, updates);

        if (errorMessage != null) {
            updateTaskErrorStatus(studentTaskId, true, fairytaleId);
        }

        return generatedId;
    }

    // ID로 프롬프트 상태 업데이트 메소드 (UPSCALE 시)
    public void updatePromptStatusById(String studentTaskId, Long fairytaleId, String imageId, CommonPromptDTO promptDTO, String status, String errorMessage) {
        DatabaseReference characterRef = getDatabaseReference(studentTaskId, promptDTO.getPromptType().name(), imageId);

        Map<String, Object> updates = createStatusUpdateMap(status, errorMessage);
        updateFirebaseData(characterRef, updates);

        if (errorMessage != null) {
            updateTaskErrorStatus(studentTaskId, true, fairytaleId);
        }
    }

    // 캐릭터 이미지 URL 업데이트 메소드
    public void updateCharacterImageUrls(String studentTaskId, String imageId, CommonPromptDTO promptDTO, List<String> imageUrls) {
        DatabaseReference characterRef = getDatabaseReference(studentTaskId, promptDTO.getPromptType().name(), imageId);

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

    // 에러 발생 시 default 이미지 반환하기
    public void updateTaskErrorStatus(String studentTaskId, boolean isError, Long fairytaleId) {
        // 이미지 업데이트
        updatePromptAndImages(studentTaskId, fairytaleId);

        // 작업 상태 및 시간 업데이트
        updateTaskStatus(studentTaskId);
    }


    // 프롬프트 상태와 이미지 URL 업데이트 메소드
    public void updatePromptAndImages(String studentTaskId, Long fairytaleId) {
        DatabaseReference taskRef = getDatabaseReference(studentTaskId);

        HashMap<String, Object> taskUpdates = new HashMap<>();
        taskUpdates.put(FirebaseFieldConstants.CHARACTER_FIELD, null);
        taskUpdates.put(FirebaseFieldConstants.BACKGROUND_FIELD, null);

        updateFirebaseData(taskRef, taskUpdates);

        // fairytaleId로 fairytale_images 테이블에서 관련 이미지 정보 조회
        List<FairytaleImage> images = fairytaleImageRepository.findByFairytaleId(fairytaleId);

        if (images != null && !images.isEmpty()) {
            for (FairytaleImage image : images) {
                List<String> imageUrlList = convertJsonArrayToList(image.getImageUrls());

                // 각 타입별로 업데이트
                Map<String, Object> typeUpdates = new HashMap<>();
                typeUpdates.put(FirebaseFieldConstants.IMAGE_URL_FIELD, imageUrlList);         // 이미지 URL 배열
                typeUpdates.put(FirebaseFieldConstants.NAME_FIELD, image.getName());                   // 이미지 이름
                typeUpdates.put(FirebaseFieldConstants.PROGRESS_FIELD, "100%");                        // 진행률 100%
                typeUpdates.put(FirebaseFieldConstants.PROMPT_FIELD, image.getPrompt());               // 프롬프트 데이터
                typeUpdates.put(FirebaseFieldConstants.STATUS_FIELD, "complete");                      // 상태 완료

                // Firebase에서 타입별로 업데이트 (예: CHARACTER, BACKGROUND)
                DatabaseReference typeRef = taskRef.child(image.getType().toUpperCase()).push();
                typeRef.updateChildrenAsync(typeUpdates);
            }
        }
    }

    // JSON 배열을 List로 변환하는 메소드
    private List<String> convertJsonArrayToList(String jsonArrayString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonArrayString, new TypeReference<List<String>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList(); // 변환 실패 시 빈 리스트 반환
        }
    }

    // 작업 에러 상태 및 완료 시간 업데이트 메소드
    public void updateTaskStatus(String studentTaskId) {
        DatabaseReference taskRef = getDatabaseReference(studentTaskId);

        // 현재 시간을 ISO 8601 형식으로 변환
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Firebase 상태 및 시간 업데이트 맵 생성
        Map<String, Object> updates = new HashMap<>();
        updates.put(FirebaseFieldConstants.ERROR_FIELD, false);   // 에러 상태
        updates.put(FirebaseFieldConstants.COMPLETED_FIELD, true);                            // 완료 상태
        updates.put(FirebaseFieldConstants.END_TIME_FIELD, currentTime);                       // 완료 시간
        updates.put(FirebaseFieldConstants.START_TIME_FIELD, currentTime);                     // 시작 시간

        // Firebase 비동기 업데이트
        taskRef.updateChildrenAsync(updates);
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

