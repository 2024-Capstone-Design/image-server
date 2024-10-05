package com.dingdong.imageserver.service.imagine;

import com.dingdong.imageserver.dto.midjourney.SubmitResultVO;
import com.dingdong.imageserver.dto.service.TaskResult;
import com.dingdong.imageserver.dto.service.CommonImageGenerationDTO;
import com.dingdong.imageserver.enums.ReturnCode;
import com.dingdong.imageserver.enums.TaskAction;
import com.dingdong.imageserver.exception.CustomException;
import com.dingdong.imageserver.model.task.Task;
import com.dingdong.imageserver.response.ErrorStatus;
import com.dingdong.imageserver.service.firebase.FirebaseUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 하나의 이미지 생성 프로세스에 대해서
 * Imagine - Fetch - Upscale - Fetch 후 생성된 image url 을 반환하는 비동기 메소드
 *
 * 1. processTask ( input : requestDTO, studentTaskId, taskId )
 * 2. submitTask  ( input : TaskAction, PromptDTO )
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MidjourneyTaskService {

    private final ThirdPartyIApiService thirdPartyIApiService;
    private final FirebaseUpdateService firebaseUpdateService;
    private final TaskStatusService taskStatusService;

    @Async("taskExecutor")
    public CompletableFuture<String> processTask(long studentTaskId, long fairytaleId, CommonImageGenerationDTO promptDTO) {

        log.info("start process : " + promptDTO);
        // Imagine
        TaskResult imagineResult = submitTask(studentTaskId, promptDTO, TaskAction.IMAGINE, null);
        log.info("imagineResult : " + imagineResult);
        // Upscale
        TaskResult upscaleResult = submitTask(studentTaskId, promptDTO, TaskAction.UPSCALE, imagineResult);
        log.info("upscaleResult : " + upscaleResult);

        Task updatedTask = thirdPartyIApiService.fetchAndSaveTask(upscaleResult.getTaskId());
        log.info("updatedTask : " + updatedTask);

        return CompletableFuture.completedFuture(updatedTask.getImageUrl());
    }

    /**
     * IMAGINE, UPSCALE 작업
     *
     */
    public TaskResult submitTask(long studentTaskId, CommonImageGenerationDTO commonImageGenerationDTO, TaskAction taskAction, TaskResult taskResult) {
        SubmitResultVO result = null;
        String imageId = null;
        String prompt = commonImageGenerationDTO.getPrompt();

        if (taskAction.equals(taskAction.IMAGINE)) {
            imageId = firebaseUpdateService.updatePromptStatus(String.valueOf(studentTaskId), commonImageGenerationDTO, prompt, "imagining");
            result = thirdPartyIApiService.submitImagineByPrompt(prompt);
        } else if (taskAction.equals(taskAction.UPSCALE)) {
            if (taskResult == null) {
                log.error("taskResult NULL 반환");
            }
            imageId = taskResult.getImageId();
            String taskId = taskResult.getTaskId();
            result = thirdPartyIApiService.submitUpscaleByTaskId(taskId);
        }

        log.info("submitTask에서 작업 수행중 : " + studentTaskId + " " + imageId +  " " +result.toString() );


        if (result.getCode() == ReturnCode.SUCCESS || result.getCode() == ReturnCode.IN_QUEUE) {

            // CompletableFuture를 사용하여 최종 결과를 반환하도록 처리
            String taskId = taskStatusService.scheduleTaskStatusFetching(
                    taskAction, result, commonImageGenerationDTO, String.valueOf(studentTaskId), imageId, prompt, taskResult);

            if (taskId != null) {
                return new TaskResult(taskId, imageId);
            }
            else {
                // Task 수행 중 오류 발생 시
                log.error("Task 수행 중 오류 발생");
                log.error("studentTaskId : " + studentTaskId);
                log.error("imageId : " + imageId);
            }

        } else {
            throw new CustomException(ErrorStatus.SERVER_ERROR, result.getResult());
        }
        throw new CustomException(ErrorStatus.SERVER_ERROR, result.getResult());
    }

}
