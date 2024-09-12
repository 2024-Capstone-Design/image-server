package com.dingdong.imageserver.service;

import com.dingdong.imageserver.dto.ImagineStatusDTO;
import com.dingdong.imageserver.dto.ImagineTaskStatusDTO;
import com.dingdong.imageserver.dto.ReImagineRequestDTO;
import com.dingdong.imageserver.dto.prompt.CommonPromptDTO;
import com.dingdong.imageserver.exception.CustomException;
import com.dingdong.imageserver.model.DataCallback;
import com.dingdong.imageserver.response.ErrorStatus;
import com.google.firebase.database.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class FirebaseService {

    private final FirebaseDatabase firebaseDatabase;

    public void initializeTaskInFirebase(Long studentTaskId) {
        DatabaseReference taskRef = firebaseDatabase.getReference(String.valueOf(studentTaskId));

        Map<String, Object> taskData = new HashMap<>();
        taskData.put("completed", false);
        taskData.put("error", false);
        taskData.put("startTime", LocalDateTime.now().toString());
        taskData.put("endTime", null);

        taskRef.setValueAsync(taskData);
    }

    // 캐릭터 레퍼런스를 얻는 메소드
    public DatabaseReference getCharacterReference(String studentTaskId, String imageId, CommonPromptDTO character) {
        return firebaseDatabase.getReference(studentTaskId)
                .child(character.getPromptType().name())
                .child(imageId);
    }

    // 프롬프트 상태 업데이트 메소드
    public String updatePromptStatus(String studentTaskId, CommonPromptDTO promptDTO, String prompt, String status, String errorMessage) {
        DatabaseReference characterRef = firebaseDatabase.getReference(studentTaskId)
                .child(promptDTO.getPromptType().name())
                .push(); // auto-increment-like key generation

        String generatedId = characterRef.getKey();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", promptDTO.getName());
        updates.put("prompt", prompt);
        updates.put("status", status);

        if (errorMessage != null) {
            DatabaseReference taskRef = firebaseDatabase.getReference(studentTaskId);
            Map<String, Object> taskData = new HashMap<>();
            taskData.put("error", true);
            taskRef.setValueAsync(taskData);

            updates.put("errorMessage", errorMessage);
        }

        characterRef.updateChildrenAsync(updates);
        return generatedId;
    }

    // ID로 프롬프트 상태 업데이트 메소드
    public void updatePromptStatusById(String studentTaskId, String generatedId, CommonPromptDTO promptDTO, String status, String errorMessage) {
        DatabaseReference characterRef = firebaseDatabase.getReference(studentTaskId)
                .child(promptDTO.getPromptType().name())
                .child(generatedId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);

        if (errorMessage != null) {
            updates.put("error", errorMessage);
        }

        characterRef.updateChildrenAsync(updates);
    }

    // 캐릭터 이미지 URL 업데이트 메소드
    public void updateCharacterImageUrls(String studentTaskId, String generatedId, CommonPromptDTO promptDTO, List<String> imageUrls) {
        DatabaseReference characterRef = firebaseDatabase.getReference(studentTaskId)
                .child(promptDTO.getPromptType().name())
                .child(generatedId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("image_url", imageUrls);
        updates.put("status", "complete");

        characterRef.updateChildrenAsync(updates);
    }

    // 특정 프롬프트 ID로 데이터 가져오는 메소드
    public void getPromptById(ReImagineRequestDTO requestDTO, DataCallback callback) {
        String studentTaskId = String.valueOf(requestDTO.getStudentTaskId());
        String imageId = String.valueOf(requestDTO.getImageId());

        DatabaseReference characterRef = firebaseDatabase.getReference(studentTaskId)
                .child(requestDTO.getPromptType().name())
                .child(imageId);

        characterRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String prompt = dataSnapshot.child("prompt").getValue(String.class);
                    String name = dataSnapshot.child("name").getValue(String.class);
                    if (prompt != null && name != null) {
                        callback.onSuccess(prompt, name);
                    } else {
                        callback.onFailure("Prompt not found");
                    }
                } else {
                    callback.onFailure("Data not found"); // 데이터가 없을 때
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onFailure(databaseError.getMessage());
            }
        });
    }

    // 작업 완료 처리 메소드
    public void finalizeTask(String studentTaskId) {
        DatabaseReference taskRef = firebaseDatabase.getReference(studentTaskId);
        taskRef.updateChildrenAsync(Map.of(
                "endTime", LocalDateTime.now().toString(),
                "completed", true
        ));
    }

    // Firebase에서 작업 상태를 조회하는 메소드
    public ImagineTaskStatusDTO getImagineStatusFromFirebase(String studentTaskId, int timeoutInSeconds)
            throws InterruptedException, ExecutionException, TimeoutException, CustomException {

        DatabaseReference taskRef = firebaseDatabase.getReference(studentTaskId);
        CompletableFuture<DataSnapshot> future = new CompletableFuture<>();

        taskRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                future.complete(snapshot);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(new RuntimeException(error.getMessage()));
            }
        });

        DataSnapshot snapshot = future.get(timeoutInSeconds, TimeUnit.SECONDS);
        if (snapshot.exists()) {
            boolean completed = snapshot.child("completed").getValue(Boolean.class);
            boolean error = snapshot.child("error").getValue(Boolean.class);
            String startTime = snapshot.child("startTime").getValue(String.class);
            String endTime = snapshot.child("endTime").getValue(String.class);

            List<ImagineStatusDTO> characters = new ArrayList<>();
            if (snapshot.child("CHARACTER").exists()) {
                for (DataSnapshot characterSnapshot : snapshot.child("CHARACTER").getChildren()) {
                    String id = characterSnapshot.getKey();
                    String name = characterSnapshot.child("name").getValue(String.class);
                    String prompt = characterSnapshot.child("prompt").getValue(String.class);
                    String status = characterSnapshot.child("status").getValue(String.class);
                    String progress = characterSnapshot.child("progress").getValue(String.class);
                    List<String> imageUrls = (List<String>) characterSnapshot.child("image_url").getValue();

                    ImagineStatusDTO characterDTO = new ImagineStatusDTO(id, name, prompt, status, progress, imageUrls);
                    characters.add(characterDTO);
                }
            }

            List<ImagineStatusDTO> backgrounds = new ArrayList<>();
            if (snapshot.child("BACKGROUND").exists()) {
                for (DataSnapshot backgroundSnapshot : snapshot.child("BACKGROUND").getChildren()) {
                    String id = backgroundSnapshot.getKey();
                    String name = backgroundSnapshot.child("name").getValue(String.class);
                    String prompt = backgroundSnapshot.child("prompt").getValue(String.class);
                    String status = backgroundSnapshot.child("status").getValue(String.class);
                    String progress = backgroundSnapshot.child("progress").getValue(String.class);
                    List<String> imageUrls = (List<String>) backgroundSnapshot.child("image_url").getValue();

                    ImagineStatusDTO backgroundDTO = new ImagineStatusDTO(id, name, prompt, status, progress, imageUrls);
                    backgrounds.add(backgroundDTO);
                }
            }

            return new ImagineTaskStatusDTO(completed, error, startTime, endTime, characters, backgrounds);
        } else {
            throw new CustomException(ErrorStatus.ENTITY_NOT_FOUND, "Data not found");
        }
    }

    // 이미지 ID로 데이터 조회하는 메소드
    public ImagineStatusDTO getImagineStatusWithImageIdFromFirebase(ReImagineRequestDTO requestDTO, int timeoutSeconds)
            throws InterruptedException, ExecutionException, TimeoutException, CustomException {
        DatabaseReference ref = firebaseDatabase.getReference(String.valueOf(requestDTO.getStudentTaskId()))
                .child(requestDTO.getPromptType().name())
                .child(requestDTO.getImageId());

        CompletableFuture<DataSnapshot> future = new CompletableFuture<>();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                future.complete(snapshot);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(new RuntimeException(error.getMessage()));
            }
        });

        DataSnapshot snapshot = future.get(timeoutSeconds, TimeUnit.SECONDS);
        if (snapshot.exists()) {
            String id = snapshot.getKey();
            String name = snapshot.child("name").getValue(String.class);
            String prompt = snapshot.child("prompt").getValue(String.class);
            String status = snapshot.child("status").getValue(String.class);
            String progress = snapshot.child("progress").getValue(String.class);
            List<String> imageUrls = (List<String>) snapshot.child("image_url").getValue();

            return new ImagineStatusDTO(id, name, prompt, status, progress, imageUrls);
        } else {
            throw new CustomException(ErrorStatus.ENTITY_NOT_FOUND, "Data not found");
        }
    }

    // 캐릭터 참조 이미지 제거 메소드
    public void clearCharacterReferenceImage(ReImagineRequestDTO requestDTO) {
        DatabaseReference taskRef = firebaseDatabase.getReference(String.valueOf(requestDTO.getStudentTaskId()));

        Map<String, Object> taskUpdates = new HashMap<>();
        taskUpdates.put("completed", false);
        taskUpdates.put("error", false);
        taskUpdates.put("startTime", LocalDateTime.now().toString());
        taskUpdates.put("endTime", null);
        taskUpdates.put("regenerate", true);

        taskRef.updateChildrenAsync(taskUpdates);

        DatabaseReference ref = firebaseDatabase.getReference(String.valueOf(requestDTO.getStudentTaskId()))
                .child(requestDTO.getPromptType().name())
                .child(requestDTO.getImageId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "imagining");
        updates.put("image_url", null);

        ref.updateChildrenAsync(updates);
    }
}
