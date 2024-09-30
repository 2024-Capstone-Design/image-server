package com.dingdong.imageserver.service.imagine;

import com.dingdong.imageserver.constant.FirebaseFieldConstants;
import com.dingdong.imageserver.dto.midjourney.SubmitResultVO;
import com.dingdong.imageserver.dto.service.CommonImageGenerationDTO;
import com.dingdong.imageserver.enums.ReturnCode;
import com.dingdong.imageserver.enums.TaskAction;
import com.dingdong.imageserver.model.task.Task;
import com.dingdong.imageserver.service.firebase.FirebaseCharacterService;
import com.dingdong.imageserver.service.firebase.FirebasePromptService;
import com.google.firebase.database.DatabaseReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskStatusService {

    private final FirebasePromptService firebasePromptService;
    private final FirebaseCharacterService firebaseCharacterService;
    private final MidjourneyApiService midjourneyApiService;
    private final AtomicInteger promptCount = new AtomicInteger(0);

    public CompletableFuture<String> scheduleTaskStatusFetching(TaskAction taskAction, SubmitResultVO submitResultVO,
                                                              CommonImageGenerationDTO character, String studentTaskId, Long fairytaleId, String imageId, String prompt) {

        final SubmitResultVO[] result = {submitResultVO};

        // CompletableFuture를 반환할 수 있도록 변경
        return CompletableFuture.supplyAsync(() -> {
            DatabaseReference characterRef = firebaseCharacterService.getCharacterReference(studentTaskId, imageId, character);
            int maxRetries = 5;  // 최대 재시도 횟수
            int retryCount = 0;  // 현재 재시도 횟수

            while (retryCount < maxRetries) {
                Task updatedTask = midjourneyApiService.fetchAndSaveTask(result[0].getResult());
                updateTaskProgress(characterRef, updatedTask);

                if (updatedTask.isSuccessful()) {
                    try {
                        handleTaskSuccess(characterRef, updatedTask, character, studentTaskId);
                        return result[0].getResult();  // 성공 시 Result Task Id 반환
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }

                } else if (updatedTask.isFailure()) {

                    // 일정 간격으로 재시도
                    retryCount++;
                    System.out.println("Task Failed, Retrying... (" + retryCount + "/" + maxRetries + ")");
                    System.out.println(result[0]);
                    if (prompt != null) {
                        if (taskAction.equals(TaskAction.IMAGINE)){
                            result[0] = midjourneyApiService.submitImagineByPrompt(prompt);
                        }
                        else if (taskAction.equals(TaskAction.UPSCALE)){
                            result[0] = midjourneyApiService.submitUpscaleByTaskId(submitResultVO.getResult());
                        }

                        sleepRandomTimeForTaskPolling();

                        if (result[0].getCode() == ReturnCode.SUCCESS || result[0].getCode() == ReturnCode.IN_QUEUE) {
                            continue;
                        } else {
                            System.out.println("다시 시도했지만 실패함");
                            System.out.println(result[0]);
                            break;
                        }
                    }
                    handleTaskFailure(taskAction, studentTaskId, fairytaleId, character, imageId, "scheduleTaskStatusFetching FAIL, prompt is null");
                    return null;
                }
                sleepRandomTimeForTaskPolling();  // 재시도 간격 대기
            }
            handleTaskFailure(taskAction, studentTaskId, fairytaleId, character, imageId, "scheduleTaskStatusFetching FAIL");
            return null;
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

    private void handleTaskSuccess(DatabaseReference characterRef,
                                   Task task, CommonImageGenerationDTO promptDTO, String studentTaskId) {
        if (task.getAction() == TaskAction.UPSCALE) {
            List<String> backgroundRemovedImageUrls = midjourneyApiService.getPostProcessingImageUrl(task.getImageUrl(), studentTaskId, promptDTO);
            characterRef.child("image_url").setValueAsync(backgroundRemovedImageUrls);
            characterRef.child("progress").setValueAsync("100%");
            characterRef.child("status").setValueAsync("complete");
        }
    }

    private void handleTaskFailure(TaskAction taskAction,
                                   String studentTaskId, Long fairytaleId, CommonImageGenerationDTO character, String imageId, String errorMessage) {
        firebasePromptService.updatePromptStatusById(studentTaskId, fairytaleId, imageId, character, "failed", taskAction.name() + " " + errorMessage);
    }

    private void sleepRandomTimeForTaskPolling() {
        try {
            // 3초(1000ms)에서 10초(5000ms) 사이의 랜덤 시간
            int randomSleepTime = ThreadLocalRandom.current().nextInt(3000, 10001);
            Thread.sleep(randomSleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
