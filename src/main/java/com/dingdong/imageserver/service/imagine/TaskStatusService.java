package com.dingdong.imageserver.service.imagine;

import com.dingdong.imageserver.constant.FirebaseFieldConstants;
import com.dingdong.imageserver.dto.midjourney.SubmitResultVO;
import com.dingdong.imageserver.dto.service.CommonImageGenerationDTO;
import com.dingdong.imageserver.dto.service.TaskResult;
import com.dingdong.imageserver.enums.ReturnCode;
import com.dingdong.imageserver.enums.TaskAction;
import com.dingdong.imageserver.exception.CustomException;
import com.dingdong.imageserver.model.task.Task;
import com.dingdong.imageserver.response.ErrorStatus;
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
        int maxRetries = 10;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            Task updatedTask = thirdPartyIApiService.fetchAndSaveTask(result[0].getResult());
            firebaseUpdateService.updateTaskProgress(characterRef, updatedTask);

            log.info(" == scheduleTaskStatusFetching while문에서 작업 수행중 == " + studentTaskId + " " + imageId + " " + result[0] + " " + updatedTask);

            if (updatedTask.isSuccessful()) {
                try {
                    firebaseUpdateService.handleTaskSuccess(characterRef, updatedTask, character, studentTaskId);
                    return result[0].getResult();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new CustomException(ErrorStatus.SERVER_ERROR, result[0].getResult() + e.getMessage());
                }
            } else if (updatedTask.isFailure()) {
                retryCount++;
                log.info("Task 실패, Retrying... (" + retryCount + "/" + maxRetries + ") " + updatedTask.getFailReason());
                if (prompt != null) {
                    result[0] = retryTask(taskAction, result[0], prompt, taskResultForUpscale, 5);
                    if (result[0] == null) {
                        firebaseUpdateService.handleTaskFailure(taskAction, studentTaskId, character, imageId, "scheduleTaskStatusFetching FAIL");
                        return null;
                    }
                } else {
                    firebaseUpdateService.handleTaskFailure(taskAction, studentTaskId, character, imageId, "scheduleTaskStatusFetching FAIL, prompt is null");
                    return null;
                }
            }
            sleepRandomTimeForTaskPolling();
        }
        firebaseUpdateService.handleTaskFailure(taskAction, studentTaskId, character, imageId, "scheduleTaskStatusFetching FAIL");
        return null;
    }

    private SubmitResultVO retryTask(TaskAction taskAction, SubmitResultVO result, String prompt, TaskResult taskResultForUpscale, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (taskAction.equals(TaskAction.IMAGINE)) {
                result = thirdPartyIApiService.submitImagineByPrompt(prompt);
            } else if (taskAction.equals(TaskAction.UPSCALE)) {
                result = thirdPartyIApiService.submitUpscaleByTaskId(taskResultForUpscale.getTaskId());
            }

            sleepRandomTimeForTaskPolling();

            if (result.getCode() == ReturnCode.SUCCESS || result.getCode() == ReturnCode.IN_QUEUE) {
                return result;
            }

            log.error("시도 " + attempt + " 실패함: " + result);

            // 최대 시도 횟수에 도달하면 null을 반환
            if (attempt == maxRetries) {
                log.error("모든 시도 실패. 더 이상 재시도하지 않음.");
                return null;
            }
        }

        return null;
    }


    private void sleepRandomTimeForTaskPolling() {
        try {
            int randomSleepTime = ThreadLocalRandom.current().nextInt(3000, 10001);
            Thread.sleep(randomSleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
