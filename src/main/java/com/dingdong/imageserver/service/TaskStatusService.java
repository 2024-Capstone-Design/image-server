package com.dingdong.imageserver.service;

import com.dingdong.imageserver.constant.FirebaseFieldConstants;
import com.dingdong.imageserver.dto.prompt.CommonPromptDTO;
import com.dingdong.imageserver.enums.TaskAction;
import com.dingdong.imageserver.model.Task;
import com.dingdong.imageserver.service.firebase.FirebaseCharacterService;
import com.dingdong.imageserver.service.firebase.FirebasePromptService;
import com.google.firebase.database.DatabaseReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskStatusService {

    private final FirebasePromptService firebasePromptService;
    private final FirebaseCharacterService firebaseCharacterService;
    private final ApiService apiService;
    private final AtomicInteger promptCount = new AtomicInteger(0);

    public void scheduleTaskStatusFetching(Boolean bgRemove, TaskAction taskAction, String taskId,
                                           Consumer<List<String>> onComplete, CommonPromptDTO character, String studentTaskId, Long fairytaleId, String imageId) {
        CompletableFuture.runAsync(() -> {
            DatabaseReference characterRef = firebaseCharacterService.getCharacterReference(studentTaskId, imageId, character);
            while (true) {
                Task updatedTask = apiService.fetchAndSaveTask(taskId);
                updateTaskProgress(characterRef, updatedTask);

                if (updatedTask.isSuccessful()) {
                    try {
                        handleTaskSuccess(onComplete, bgRemove,  characterRef, updatedTask, character, studentTaskId);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    break;
                } else if (updatedTask.isFailure()) {
                    handleTaskFailure(onComplete, taskAction, studentTaskId, fairytaleId, character, imageId);
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
        characterRef.child(FirebaseFieldConstants.PROGRESS_FIELD).setValueAsync(task.getProgress());
    }

    private void handleTaskSuccess(Consumer<List<String>> onComplete, Boolean bgRemove, DatabaseReference characterRef,
                                   Task task, CommonPromptDTO promptDTO, String studentTaskId) {

        if (task.getAction() == TaskAction.UPSCALE && bgRemove) {
            List<String> backgroundRemovedImageUrls = apiService.getPostProcessingImageUrl(task.getImageUrl(), studentTaskId, promptDTO);
            characterRef.child(FirebaseFieldConstants.IMAGE_URL_FIELD).setValueAsync(backgroundRemovedImageUrls);
            characterRef.child(FirebaseFieldConstants.PROGRESS_FIELD).setValueAsync("100%");
            characterRef.child(FirebaseFieldConstants.STATUS_FIELD).setValueAsync("complete");
            onComplete.accept(backgroundRemovedImageUrls);
        } else {
            onComplete.accept(null);
        }
    }

    private void handleTaskFailure(Consumer<List<String>> onComplete, TaskAction taskAction,
                                   String studentTaskId, Long fairytaleId, CommonPromptDTO character, String imageId) {
        firebasePromptService.updatePromptStatusById(studentTaskId, fairytaleId, imageId, character, "failed", taskAction.name() + " task failed");
        onComplete.accept(null);
    }

    private void sleepForTaskPolling() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
