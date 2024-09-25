package com.dingdong.imageserver.service.firebase;

import com.dingdong.imageserver.constant.FirebaseFieldConstants;
import com.google.firebase.database.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class FirebaseTaskService extends FirebaseBaseService {

    public FirebaseTaskService(FirebaseDatabase firebaseDatabase) {
        super(firebaseDatabase);
    }

    public void initializeTaskInFirebase(Long studentTaskId, Consumer<Boolean> callback) {
        DatabaseReference taskRef = getDatabaseReference(String.valueOf(studentTaskId));

        // complete 값 확인
        taskRef.child(FirebaseFieldConstants.COMPLETED_FIELD).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Boolean isComplete = dataSnapshot.getValue(Boolean.class);

                if (dataSnapshot.exists() && Boolean.FALSE.equals(isComplete)) {
                    // complete 값이 존재하는데 미완성일 경우 (false일 경우) 콜백으로 실패를 알림
                    System.out.println("이미 진행중인 Task 입니다.");
                    callback.accept(false);
                    return;
                }

                // Task 초기화 - 값을 설정할 때는 setValueAsync 사용
                Map<String, Object> taskData = createTaskInitializationData();
                taskRef.setValueAsync(taskData);
                callback.accept(true);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // 에러 처리: Firebase 작업이 실패하면 콜백으로 false 전달
                callback.accept(false);
            }
        });
    }



    // 작업 완료 처리 메소드
    public void finalizeTask(String studentTaskId) {
        DatabaseReference taskRef = getDatabaseReference(studentTaskId);
        Map<String, Object> updateData = Map.of(
                FirebaseFieldConstants.END_TIME_FIELD, LocalDateTime.now().toString(),
                FirebaseFieldConstants.COMPLETED_FIELD, true
        );
        taskRef.updateChildrenAsync(updateData);
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
}
