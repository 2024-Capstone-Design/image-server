package com.dingdong.imageserver.service.firebase;

import com.google.firebase.database.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class FirebaseBaseService {

    protected final FirebaseDatabase firebaseDatabase;

    public FirebaseBaseService(FirebaseDatabase firebaseDatabase) {
        this.firebaseDatabase = firebaseDatabase;
    }

    // Firebase 참조를 얻는 공통 메소드
    protected DatabaseReference getDatabaseReference(String... paths) {
        DatabaseReference ref = firebaseDatabase.getReference();
        for (String path : paths) {
            ref = ref.child(path);
        }
        return ref;
    }

    // Firebase 데이터 업데이트
    protected void updateFirebaseData(DatabaseReference ref, Map<String, Object> updates) {
        ref.updateChildrenAsync(updates);
    }

    // Firebase 데이터 조회 및 스냅샷 획득
    protected DataSnapshot getDataSnapshot(DatabaseReference ref, int timeoutInSeconds)
            throws InterruptedException, ExecutionException, TimeoutException {

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

        return future.get(timeoutInSeconds, TimeUnit.SECONDS);
    }
}

