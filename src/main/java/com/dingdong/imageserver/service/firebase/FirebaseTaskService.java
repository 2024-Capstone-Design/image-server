package com.dingdong.imageserver.service.firebase;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class FirebaseTaskService extends FirebaseBaseService {

    public FirebaseTaskService(FirebaseDatabase firebaseDatabase) {
        super(firebaseDatabase);
    }

    // 작업 초기화 메소드
    public void initializeTaskInFirebase(Long studentTaskId) {
        DatabaseReference taskRef = getDatabaseReference(String.valueOf(studentTaskId));

        Map<String, Object> taskData = new HashMap<>();
        taskData.put("completed", false);
        taskData.put("error", false);
        taskData.put("startTime", LocalDateTime.now().toString());
        taskData.put("endTime", null);

        taskRef.setValueAsync(taskData);
    }

    // 작업 완료 처리 메소드
    public void finalizeTask(String studentTaskId) {
        DatabaseReference taskRef = getDatabaseReference(studentTaskId);
        taskRef.updateChildrenAsync(Map.of(
                "endTime", LocalDateTime.now().toString(),
                "completed", true
        ));
    }

}

