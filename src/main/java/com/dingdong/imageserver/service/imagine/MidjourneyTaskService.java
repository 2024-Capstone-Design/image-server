package com.dingdong.imageserver.service.imagine;

import com.dingdong.imageserver.dto.midjourney.SubmitResultVO;
import com.dingdong.imageserver.dto.service.TaskResult;
import com.dingdong.imageserver.dto.service.CommonImageGenerationDTO;
import com.dingdong.imageserver.enums.ReturnCode;
import com.dingdong.imageserver.enums.TaskAction;
import com.dingdong.imageserver.enums.TaskType;
import com.dingdong.imageserver.model.task.Task;
import com.dingdong.imageserver.service.firebase.FirebasePromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

    private final MidjourneyApiService apiService;
    private final FirebasePromptService firebasePromptService;
    private final TaskStatusService taskStatusService;

    public String processTask(long studentTaskId, long fairytaleId, CommonImageGenerationDTO promptDTO) {

        // Imagine
        TaskResult imagineResult = submitTask(studentTaskId, fairytaleId, promptDTO, TaskType.IMAGINE, null);
        // Upscale
        TaskResult upscaleResult = submitTask(studentTaskId, fairytaleId, promptDTO, TaskType.UPSCALE, imagineResult);

        Task updatedTask = apiService.fetchAndSaveTask(upscaleResult.getTaskId());

        return updatedTask.getImageUrl();

    }

    /**
     * IMAGINE 작업
     *
     */
    public TaskResult submitTask(long studentTaskId, long fairytaleId, CommonImageGenerationDTO protagonist, TaskType taskType, TaskResult taskResult) {
        SubmitResultVO result = null;
        String imageId = null;
        String prompt = protagonist.getPrompt();

        if (taskType.equals(TaskType.IMAGINE)) {
            imageId = firebasePromptService.updatePromptStatus(String.valueOf(studentTaskId), fairytaleId, protagonist, prompt, "imagining", null);
            result = apiService.submitImagineByPrompt(prompt);
        } else if (taskType.equals(TaskType.UPSCALE)) {
            if (taskResult == null) {
                System.out.println("taskResult NULL 반환");
            }
            imageId = taskResult.getImageId();
            String taskId = taskResult.getTaskId();
            result = apiService.submitUpscaleByTaskId(taskId);
        }

        // TODO : 대기 메소드 추가하기
        if (result.getCode() == ReturnCode.SUCCESS || result.getCode() == ReturnCode.IN_QUEUE) {

            // CompletableFuture를 사용하여 최종 결과를 반환하도록 처리
            CompletableFuture<String> taskIdFuture = taskStatusService.scheduleTaskStatusFetching(
                    TaskAction.IMAGINE, result, protagonist, String.valueOf(studentTaskId), fairytaleId, imageId, prompt
            );

            try {
                // 비동기 작업의 결과를 기다리고 반환
                String updatedTaskId = taskIdFuture.get();  // get()을 호출하면 완료될 때까지 대기
                if (updatedTaskId != null) {
                    return new TaskResult(updatedTaskId, imageId);
                }
                else {
                    // Task 수행 중 오류 발생 시
                    log.error("Task 수행 중 오류 발생");
                    log.error("studentTaskId : " + studentTaskId);
                    log.error("imageId : " + imageId);
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException(result.getResult());
        }
        throw new RuntimeException(result.getResult());
    }

}
