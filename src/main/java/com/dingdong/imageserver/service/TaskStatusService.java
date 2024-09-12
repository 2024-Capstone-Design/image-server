package com.dingdong.imageserver.service;

import com.dingdong.imageserver.dto.prompt.CommonPromptDTO;
import com.dingdong.imageserver.enums.TaskAction;
import com.dingdong.imageserver.model.Task;
import com.google.firebase.database.DatabaseReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class TaskStatusService {

    private final FirebaseService firebaseService;
    private final ApiService apiService;
    private final AtomicInteger promptCount = new AtomicInteger(0);

    public void scheduleTaskStatusFetching(Boolean bgRemove, TaskAction taskAction, String taskId,
                                           Consumer<List<String>> onComplete, CommonPromptDTO character, String studentTaskId, String imageId) {
        CompletableFuture.runAsync(() -> {
            DatabaseReference characterRef = firebaseService.getCharacterReference(studentTaskId, imageId, character);
            while (true) {
                Task updatedTask = apiService.fetchAndSaveTask(taskId);
                updateTaskProgress(characterRef, updatedTask);

                if (updatedTask.isSuccessful()) {
                    try {
                        handleTaskSuccess(bgRemove, onComplete, characterRef, updatedTask, character, studentTaskId, imageId);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    break;
                } else if (updatedTask.isFailure()) {
                    handleTaskFailure(characterRef, taskAction, studentTaskId, character, imageId);
                    break;
                }
                sleepForTaskPolling();
            }
        });
    }

    public int decrementPromptCount() {
        return promptCount.decrementAndGet();
    }

    public void setPromptCount(int count) {
        promptCount.set(count);
    }

    private void updateTaskProgress(DatabaseReference characterRef, Task task) {
        characterRef.child("progress").setValueAsync(task.getProgress());
    }

    private void handleTaskSuccess(Boolean bgRemove, Consumer<List<String>> onComplete, DatabaseReference characterRef,
                                   Task task, CommonPromptDTO promptDTO, String studentTaskId, String imageId) throws Exception {
        if (task.getAction() == TaskAction.UPSCALE && bgRemove) {
            List<String> backgroundRemovedImageUrls = apiService.getPostProcessingImageUrl(task.getImageUrl(), studentTaskId, promptDTO);
            characterRef.child("image_url").setValueAsync(backgroundRemovedImageUrls);
            characterRef.child("progress").setValueAsync("100%");
            characterRef.child("status").setValueAsync("complete");
            onComplete.accept(backgroundRemovedImageUrls);
        } else {
            onComplete.accept(null);
        }
    }

    private void handleTaskFailure(DatabaseReference characterRef, TaskAction taskAction,
                                   String studentTaskId, CommonPromptDTO character, String imageId) {
        firebaseService.updatePromptStatusById(studentTaskId, imageId, character, "failed", taskAction.name() + " task failed");
        characterRef.child("progress").setValueAsync("-1");
    }

    private void sleepForTaskPolling() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
