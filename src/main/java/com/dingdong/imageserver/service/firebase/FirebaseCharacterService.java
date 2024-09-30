package com.dingdong.imageserver.service.firebase;

import com.dingdong.imageserver.constant.FirebaseFieldConstants;
import com.dingdong.imageserver.dto.request.ReImagineRequestDTO;
import com.dingdong.imageserver.dto.service.CommonImageGenerationDTO;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;

@Service
@Slf4j
public class FirebaseCharacterService extends FirebaseBaseService {

    public FirebaseCharacterService(FirebaseDatabase firebaseDatabase) {
        super(firebaseDatabase);
    }

    // 캐릭터 참조 이미지 제거 메소드
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

    // 캐릭터 레퍼런스 반환 메서드
    public DatabaseReference getCharacterReference(String studentTaskId, String imageId, CommonImageGenerationDTO character) {
        return getDatabaseReference(studentTaskId, character.getPromptType().name(), imageId);
    }
}
