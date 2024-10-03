package com.dingdong.imageserver.service.imagine;

import com.dingdong.imageserver.constant.FirebaseFieldConstants;
import com.dingdong.imageserver.dto.midjourney.SubmitResultVO;
import com.dingdong.imageserver.dto.service.CommonImageGenerationDTO;
import com.dingdong.imageserver.dto.service.TaskResult;
import com.dingdong.imageserver.enums.ReturnCode;
import com.dingdong.imageserver.enums.TaskAction;
import com.dingdong.imageserver.model.task.Task;
import com.dingdong.imageserver.service.firebase.FirebaseUpdateService;
import com.google.firebase.database.DatabaseReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskStatusService {

    private final FirebaseUpdateService firebaseUpdateService;
    private final ThirdPartyIApiService thirdPartyIApiService;

    public String scheduleTaskStatusFetching(TaskAction taskAction, SubmitResultVO submitResultVO,
                                             CommonImageGenerationDTO character, String studentTaskId,
                                             String imageId, String prompt,
                                             TaskResult taskResultForUpscale) {

        final SubmitResultVO[] result = {submitResultVO};
        log.info("scheduleTaskStatusFetching 작업 수행중 : " + studentTaskId + " " + imageId);

        DatabaseReference characterRef = firebaseUpdateService.getCharacterReference(studentTaskId, imageId, character);
        int maxRetries = 10;  // 최대 재시도 횟수
        int retryCount = 0;  // 현재 재시도 횟수

        while (retryCount < maxRetries) {
            Task updatedTask = thirdPartyIApiService.fetchAndSaveTask(result[0].getResult());
            updateTaskProgress(characterRef, updatedTask);

            log.info("scheduleTaskStatusFetching while문에서 작업 수행중 : " + studentTaskId + " " + imageId + " " + result[0] + " " + updatedTask);

            if (updatedTask.isSuccessful()) {
                try {
                    handleTaskSuccess(characterRef, updatedTask, character, studentTaskId);
                    return result[0].getResult();  // 성공 시 Task Id 반환
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

            } else if (updatedTask.isFailure()) {
                retryCount++;
                log.info("Task Failed, Retrying... (" + retryCount + "/" + maxRetries + ")");
                if (prompt != null) {
                    if (taskAction.equals(TaskAction.IMAGINE)) {
                        result[0] = thirdPartyIApiService.submitImagineByPrompt(prompt);
                    } else if (taskAction.equals(TaskAction.UPSCALE)) {
                        result[0] = thirdPartyIApiService.submitUpscaleByTaskId(taskResultForUpscale.getTaskId());
                    }

                    sleepRandomTimeForTaskPolling();

                    if (result[0].getCode() == ReturnCode.SUCCESS || result[0].getCode() == ReturnCode.IN_QUEUE) {
                        log.info(studentTaskId + imageId + " 첫번째 다시 시도해서 성공");
                        continue;
                    } else {
                        log.error(studentTaskId + imageId + " 첫번째 다시 시도했지만 실패함 " + result[0]);
                        if (taskAction.equals(TaskAction.IMAGINE)) {
                            result[0] = thirdPartyIApiService.submitImagineByPrompt(prompt);
                        } else if (taskAction.equals(TaskAction.UPSCALE)) {
                            result[0] = thirdPartyIApiService.submitUpscaleByTaskId(taskResultForUpscale.getTaskId());
                        }

                        sleepRandomTimeForTaskPolling();

                        if (result[0].getCode() == ReturnCode.SUCCESS || result[0].getCode() == ReturnCode.IN_QUEUE) {
                            continue;
                        } else {
                            log.error(studentTaskId + imageId + " 두번째 다시 시도했지만 실패함 " + result[0]);
                            handleTaskFailure(taskAction, studentTaskId, character, imageId, "scheduleTaskStatusFetching FAIL");
                            return null;
                        }
                    }
                }
                else {
                    log.error(studentTaskId + imageId + " TASK FAIL scheduleTaskStatusFetching FAIL, prompt is null");
                    handleTaskFailure(taskAction, studentTaskId, character, imageId, "scheduleTaskStatusFetching FAIL, prompt is null");
                    return null;
                }
            }
            sleepRandomTimeForTaskPolling();  // 재시도 간격 대기
        }
        log.error(studentTaskId + imageId + " TASK FAIL scheduleTaskStatusFetching FAIL");
        handleTaskFailure(taskAction, studentTaskId, character, imageId, "scheduleTaskStatusFetching FAIL");
        return null;
    }

    private void updateTaskProgress(DatabaseReference characterRef, Task task) {
        characterRef.child(FirebaseFieldConstants.PROGRESS_FIELD).setValueAsync(task.getProgress());
    }

    private void handleTaskSuccess(DatabaseReference characterRef,
                                   Task task, CommonImageGenerationDTO promptDTO, String studentTaskId) {
        if (task.getAction() == TaskAction.UPSCALE) {
            List<String> backgroundRemovedImageUrls = thirdPartyIApiService.getPostProcessingImageUrl(task.getImageUrl(), studentTaskId, promptDTO);
            characterRef.child(FirebaseFieldConstants.IMAGE_URL_FIELD).setValueAsync(backgroundRemovedImageUrls);
            characterRef.child(FirebaseFieldConstants.PROGRESS_FIELD).setValueAsync("100%");
            characterRef.child(FirebaseFieldConstants.STATUS_FIELD).setValueAsync("complete");
            characterRef.child(FirebaseFieldConstants.END_TIME_FIELD).setValueAsync(LocalDateTime.now().toString());
        }
    }

    private void handleTaskFailure(TaskAction taskAction,
                                   String studentTaskId, CommonImageGenerationDTO character, String imageId, String errorMessage) {
        firebaseUpdateService.updateErrorStatusById(studentTaskId, imageId, character, "failed", taskAction.name() + " " + errorMessage);
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
