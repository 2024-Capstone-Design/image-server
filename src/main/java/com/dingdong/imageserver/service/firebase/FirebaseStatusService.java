package com.dingdong.imageserver.service.firebase;

import com.dingdong.imageserver.constant.FirebaseFieldConstants;
import com.dingdong.imageserver.dto.firebase.ImagineStatusDTO;
import com.dingdong.imageserver.dto.firebase.ImagineTaskStatusDTO;
import com.dingdong.imageserver.dto.request.ReImagineRequestDTO;
import com.dingdong.imageserver.exception.CustomException;
import com.dingdong.imageserver.response.ErrorStatus;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
@Service
public class FirebaseStatusService extends FirebaseBaseService {

    public FirebaseStatusService(FirebaseDatabase firebaseDatabase) {
        super(firebaseDatabase);
    }

    // Firebase에서 작업 상태를 조회하는 메소드
    public ImagineTaskStatusDTO getImagineStatusFromFirebase(String studentTaskId, int timeoutInSeconds)
            throws InterruptedException, ExecutionException, TimeoutException, CustomException {

        DataSnapshot snapshot = getDataSnapshot(getDatabaseReference(studentTaskId), timeoutInSeconds);
        if (!snapshot.exists()) {
            throw new CustomException(ErrorStatus.ENTITY_NOT_FOUND, studentTaskId + " data not found");
        }

        boolean completed = getBooleanFromSnapshot(snapshot, FirebaseFieldConstants.COMPLETED_FIELD);
        boolean error = getBooleanFromSnapshot(snapshot, FirebaseFieldConstants.ERROR_FIELD);
        String startTime = getStringFromSnapshot(snapshot, FirebaseFieldConstants.START_TIME_FIELD);
        String endTime = getStringFromSnapshot(snapshot, FirebaseFieldConstants.END_TIME_FIELD);

        List<ImagineStatusDTO> characters = getPromptListFromSnapshot(snapshot, FirebaseFieldConstants.CHARACTER_FIELD);
        List<ImagineStatusDTO> backgrounds = getPromptListFromSnapshot(snapshot, FirebaseFieldConstants.BACKGROUND_FIELD);

        return new ImagineTaskStatusDTO(completed, error, startTime, endTime, characters, backgrounds);
    }

    // 이미지 ID로 데이터 조회하는 메소드
    public ImagineStatusDTO getImagineStatusWithImageIdFromFirebase(ReImagineRequestDTO requestDTO, int timeoutSeconds)
            throws InterruptedException, ExecutionException, TimeoutException, CustomException {

        DatabaseReference ref = getDatabaseReference(
                String.valueOf(requestDTO.getStudentTaskId()),
                requestDTO.getPromptType().name(),
                requestDTO.getImageId()
        );

        DataSnapshot snapshot = getDataSnapshot(ref, timeoutSeconds);
        if (!snapshot.exists()) {
            throw new CustomException(ErrorStatus.ENTITY_NOT_FOUND, "Data not found");
        }

        return getImagineStatusFromSnapshot(snapshot);
    }

    // 스냅샷에서 프롬프트 리스트 추출
    private List<ImagineStatusDTO> getPromptListFromSnapshot(DataSnapshot snapshot, String promptType) {
        List<ImagineStatusDTO> prompts = new ArrayList<>();
        if (snapshot.child(promptType).exists()) {
            for (DataSnapshot promptSnapshot : snapshot.child(promptType).getChildren()) {
                prompts.add(getImagineStatusFromSnapshot(promptSnapshot));
            }
        }
        return prompts;
    }

    // 스냅샷에서 ImagineStatusDTO 생성
    private ImagineStatusDTO getImagineStatusFromSnapshot(DataSnapshot snapshot) {
        String id = snapshot.getKey();
        String name = getStringFromSnapshot(snapshot, FirebaseFieldConstants.NAME_FIELD);
        String prompt = getStringFromSnapshot(snapshot, FirebaseFieldConstants.PROMPT_FIELD);
        String errorMessage = getStringFromSnapshot(snapshot, FirebaseFieldConstants.ERROR_MESSAGE_FIELD);
        String status = getStringFromSnapshot(snapshot, FirebaseFieldConstants.STATUS_FIELD);
        String progress = getStringFromSnapshot(snapshot, FirebaseFieldConstants.PROGRESS_FIELD);
        List<String> imageUrls = getListFromSnapshot(snapshot, FirebaseFieldConstants.IMAGE_URL_FIELD);

        return new ImagineStatusDTO(id, name, prompt, status, errorMessage, progress, imageUrls);
    }

    // 공통 스냅샷 처리 메소드
    private String getStringFromSnapshot(DataSnapshot snapshot, String field) {
        return snapshot.child(field).exists() ? snapshot.child(field).getValue(String.class) : null;
    }

    private boolean getBooleanFromSnapshot(DataSnapshot snapshot, String field) {
        return snapshot.child(field).exists() ? snapshot.child(field).getValue(Boolean.class) != null && snapshot.child(field).getValue(Boolean.class) : false;
    }

    private List<String> getListFromSnapshot(DataSnapshot snapshot, String field) {
        if (snapshot.child(field).exists()) {
            Object value = snapshot.child(field).getValue();
            if (value instanceof List) {
                return (List<String>) value;
            } else if (value instanceof String) {
                // 문자열을 리스트로 변환
                return Collections.singletonList((String) value);
            }
        }
        return new ArrayList<>();
    }
}
