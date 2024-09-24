package com.dingdong.imageserver.service.firebase;

import com.dingdong.imageserver.constant.FirebaseFieldConstants;
import com.dingdong.imageserver.dto.ReImagineRequestDTO;
import com.dingdong.imageserver.dto.prompt.CommonPromptDTO;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
@Service
public class FirebaseCharacterService extends FirebaseBaseService {

    public FirebaseCharacterService(FirebaseDatabase firebaseDatabase) {
        super(firebaseDatabase);
    }

    // 캐릭터 참조 이미지 제거 메소드
    public void clearCharacterReferenceImage(ReImagineRequestDTO requestDTO) {
        DatabaseReference taskRef = getDatabaseReference(String.valueOf(requestDTO.getStudentTaskId()));

        Map<String, Object> taskUpdates = Map.of(
                FirebaseFieldConstants.COMPLETED_FIELD, false,
                FirebaseFieldConstants.ERROR_FIELD, false,
                FirebaseFieldConstants.START_TIME_FIELD, LocalDateTime.now().toString(),
                FirebaseFieldConstants.END_TIME_FIELD, null,
                FirebaseFieldConstants.REGENERATE_FIELD, true
        );

        updateFirebaseData(taskRef, taskUpdates);

        DatabaseReference ref = getDatabaseReference(
                String.valueOf(requestDTO.getStudentTaskId()),
                requestDTO.getPromptType().name(),
                requestDTO.getImageId()
        );

        Map<String, Object> updates = Map.of(
                FirebaseFieldConstants.STATUS_FIELD, "imagining",
                FirebaseFieldConstants.IMAGE_URL_FIELD, null
        );

        updateFirebaseData(ref, updates);
    }

    // 캐릭터 레퍼런스 반환 메서드
    public DatabaseReference getCharacterReference(String studentTaskId, String imageId, CommonPromptDTO character) {
        return getDatabaseReference(studentTaskId, character.getPromptType().name(), imageId);
    }
}
