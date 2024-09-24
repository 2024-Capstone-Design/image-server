package com.dingdong.imageserver.service;

import com.dingdong.imageserver.dto.*;
import com.dingdong.imageserver.dto.prompt.BackgroundDTO;
import com.dingdong.imageserver.dto.prompt.CharacterDTO;
import com.dingdong.imageserver.dto.prompt.CommonPromptDTO;
import com.dingdong.imageserver.dto.prompt.PromptType;
import com.dingdong.imageserver.enums.ReturnCode;
import com.dingdong.imageserver.enums.TaskAction;
import com.dingdong.imageserver.exception.CustomException;
import com.dingdong.imageserver.model.DataCallback;
import com.dingdong.imageserver.model.Option;
import com.dingdong.imageserver.model.OptionRepository;
import com.dingdong.imageserver.response.ErrorStatus;
import com.dingdong.imageserver.service.firebase.FirebaseCharacterService;
import com.dingdong.imageserver.service.firebase.FirebasePromptService;
import com.dingdong.imageserver.service.firebase.FirebaseStatusService;
import com.dingdong.imageserver.service.firebase.FirebaseTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final FirebaseTaskService firebaseTaskService;
    private final FirebasePromptService firebasePromptService;
    private final FirebaseStatusService firebaseStatusService;
    private final FirebaseCharacterService firebaseCharacterService;
    private final ApiService apiService;
    private final TaskStatusService taskStatusService;
    private final OptionRepository optionRepository;

    public SubmitResultVO imagine(ImagineRequestDTO requestDTO) {
        if (isInvalidRequest(requestDTO)) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "Invalid request");
        }

        CommonPromptDTO protagonist = findProtagonist(requestDTO.getCharacters());
        if (protagonist == null) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "No protagonist found");
        }

        List<CommonPromptDTO> prompts = new ArrayList<>();
        prompts.addAll(findNonProtagonists(requestDTO.getCharacters()));
        prompts.addAll(findBackgrounds(requestDTO.getBackgrounds()));

        // Task 초기화
        firebaseTaskService.initializeTaskInFirebase(requestDTO.getStudentTaskId());
        String initialPrompt = createInitialPrompt(protagonist, requestDTO.getSketchImage(), requestDTO.getOptionIds());

        SubmitResultVO result = processImagineTask(protagonist, initialPrompt, prompts, String.valueOf(requestDTO.getStudentTaskId()));
        return result;
    }

    private SubmitResultVO processImagineTask(CommonPromptDTO protagonist, String initialPrompt, List<CommonPromptDTO> prompts, String studentTaskId) {
        SubmitResultVO result = apiService.submitImagineByPrompt(initialPrompt);
        if (result.getCode() == ReturnCode.SUCCESS) {
            String imageId = firebasePromptService.updatePromptStatus(studentTaskId, protagonist, initialPrompt, "imagining", null);
            taskStatusService.scheduleTaskStatusFetching(
                    false, TaskAction.IMAGINE, result.getResult().toString(),
                    bgRemovedImageUrl -> handleImagineComplete(true, result, prompts, studentTaskId, protagonist, imageId),
                    protagonist, studentTaskId, imageId
            );
        } else {
            firebasePromptService.updatePromptStatus(studentTaskId, protagonist, initialPrompt, "failed", "Failed to start imagine task");
        }
        return result;
    }

    private void handleImagineComplete(Boolean isContinue, SubmitResultVO result, List<CommonPromptDTO> prompts, String studentTaskId, CommonPromptDTO character, String imageId) {
        SubmitResultVO upscaleResult = upscaleCharacterImage(result.getResult().toString());
        if (upscaleResult.getCode() == ReturnCode.SUCCESS) {
            taskStatusService.scheduleTaskStatusFetching(
                    true, TaskAction.UPSCALE, (String) upscaleResult.getResult(),
                    bgRemovedImageUrl -> handleUpscaleComplete(isContinue, prompts, studentTaskId, bgRemovedImageUrl),
                    character, studentTaskId, imageId
            );
        } else {
            firebasePromptService.updatePromptStatusById(studentTaskId, imageId, character, "failed", "Failed to start upscale task");
        }
    }

    private SubmitResultVO upscaleCharacterImage(String taskId) {
        SubmitChangeDTO submitChangeDTO = createSubmitChangeDTO(taskId, 2, TaskAction.UPSCALE);
        return apiService.changeTask(submitChangeDTO);
    }

    private SubmitChangeDTO createSubmitChangeDTO(String taskId, int index, TaskAction action) {
        SubmitChangeDTO submitChangeDTO = new SubmitChangeDTO();
        submitChangeDTO.setIndex(index);
        submitChangeDTO.setAction(action);
        submitChangeDTO.setTaskId(taskId);
        return submitChangeDTO;
    }


    private void handleUpscaleComplete(Boolean isContinue, List<CommonPromptDTO> prompts, String studentTaskId, List<String> bgRemovedImageUrls) {
        if (isContinue) {
            processRemainingPrompts(prompts, bgRemovedImageUrls, studentTaskId);
        } else if (taskStatusService.decrementPromptCount() <= 0) {
            firebaseTaskService.finalizeTask(studentTaskId);
        }
    }

    private void processRemainingPrompts(List<CommonPromptDTO> prompts, List<String> bgRemovedImageUrls, String studentTaskId) {
        taskStatusService.setPromptCount(prompts.size());
        prompts.forEach(prompt -> {
            processPromptTask(prompt, bgRemovedImageUrls.get(0), studentTaskId);
            pauseBetweenTasks();
        });
    }

    private void processPromptTask(CommonPromptDTO promptDTO, String referenceImage, String studentTaskId) {
        String prompt = createPrompt(promptDTO, referenceImage);
        SubmitResultVO result = apiService.submitImagineByPrompt(prompt);
        if (result.getCode() == ReturnCode.SUCCESS) {
            String imageId = firebasePromptService.updatePromptStatus(studentTaskId, promptDTO, prompt, "imagining", null);
            taskStatusService.scheduleTaskStatusFetching(
                    false, TaskAction.IMAGINE, (String) result.getResult(),
                    bgRemovedImageUrl -> handleNonProtagonistComplete(result, promptDTO, studentTaskId, imageId),
                    promptDTO, studentTaskId, imageId
            );
        } else {
            firebasePromptService.updatePromptStatus(studentTaskId, promptDTO, prompt, "failed", "Failed to start imagine task");
        }
    }

    private void handleNonProtagonistComplete(SubmitResultVO result, CommonPromptDTO promptDTO, String studentTaskId, String imageId) {
        SubmitResultVO upscaleResult = upscaleCharacterImage(result.getResult().toString());
        if (upscaleResult.getCode() == ReturnCode.SUCCESS) {
            taskStatusService.scheduleTaskStatusFetching(
                    true, TaskAction.UPSCALE, (String) upscaleResult.getResult(),
                    bgRemovedImageUrl -> finalizeCharacterImage(promptDTO, studentTaskId, bgRemovedImageUrl, imageId),
                    promptDTO, studentTaskId, imageId
            );
        } else {
            firebasePromptService.updatePromptStatusById(studentTaskId, imageId, promptDTO, "failed", "Failed to start upscale task");
        }
    }

    private void finalizeCharacterImage(CommonPromptDTO promptDTO, String studentTaskId, List<String> bgRemovedImageUrls, String imageId) {
        firebasePromptService.updateCharacterImageUrls(studentTaskId, imageId, promptDTO, bgRemovedImageUrls);
        if (taskStatusService.decrementPromptCount() <= 0) {
            firebaseTaskService.finalizeTask(studentTaskId);
        }
    }

    private void pauseBetweenTasks() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 기존 메소드는 유지하지만 기능 중심으로 간결화 및 분리
    private boolean isInvalidRequest(ImagineRequestDTO requestDTO) {
        return requestDTO.getCharacters() == null || requestDTO.getCharacters().isEmpty();
    }

    private CommonPromptDTO findProtagonist(List<CharacterDTO> characters) {
        return characters.stream()
                .filter(CharacterDTO::isMain)
                .findFirst()
                .map(c -> new CommonPromptDTO(PromptType.CHARACTER, c.getName(), c.getPrompt()))
                .orElse(null);
    }

    private List<CommonPromptDTO> findNonProtagonists(List<CharacterDTO> characters) {
        return characters.stream()
                .filter(c -> !c.isMain())
                .map(c -> new CommonPromptDTO(PromptType.CHARACTER, c.getName(), c.getPrompt()))
                .collect(Collectors.toList());
    }

    private List<CommonPromptDTO> findBackgrounds(List<BackgroundDTO> backgrounds) {
        return backgrounds.stream()
                .map(b -> new CommonPromptDTO(PromptType.BACKGROUND, b.getName(), b.getPrompt()))
                .collect(Collectors.toList());
    }

    private String createInitialPrompt(CommonPromptDTO protagonist, String sketchImage, List<Long> optionIds) {
        StringBuilder prompt = new StringBuilder(apiService.getPromptValueFromDatabase("character-content"))
                .append(protagonist.getPrompt());

        List<Option> selectedOptions = optionRepository.findAllById(optionIds);
        for (Option option : selectedOptions) {
            prompt.append(" ").append(option.getPrompt());
        }

        prompt.append(apiService.getPromptValueFromDatabase("character-style"))
                .append(apiService.getPromptValueFromDatabase("character-parameter"));

        if (sketchImage != null) {
            prompt.append(" --cref ").append(sketchImage);
        }
        return prompt.toString();
    }

    private String createPrompt(CommonPromptDTO promptDTO, String referenceImage) {
        StringBuilder prompt = new StringBuilder();
        if (promptDTO.getPromptType().equals(PromptType.CHARACTER)) {
            prompt.append(apiService.getPromptValueFromDatabase("character-content"))
                    .append(promptDTO.getPrompt())
                    .append(apiService.getPromptValueFromDatabase("character-style"))
                    .append(apiService.getPromptValueFromDatabase("character-parameter"));
        } else {
            prompt.append(apiService.getPromptValueFromDatabase("background-content"))
                    .append(promptDTO.getPrompt())
                    .append(apiService.getPromptValueFromDatabase("background-style"))
                    .append(apiService.getPromptValueFromDatabase("background-parameter"));
        }
        return prompt.append(" --sref ").append(referenceImage).toString();
    }

    public ImagineTaskStatusDTO getImagineStatus(String studentTaskId) {
        try {
            return firebaseStatusService.getImagineStatusFromFirebase(studentTaskId, 10);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new CustomException(ErrorStatus.SERVER_ERROR, e.getMessage());
        }
    }

    public ImagineStatusDTO getImagineStatusWithImageId(ReImagineRequestDTO requestDTO) {
        try {
            return firebaseStatusService.getImagineStatusWithImageIdFromFirebase(requestDTO, 10);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new CustomException(ErrorStatus.SERVER_ERROR, e.getMessage());
        }
    }


    public void regenerate(ReImagineRequestDTO requestDTO, DataCallback callback) {
        // Firebase에서 해당 프롬프트를 가져오는 작업 (FirebasePromptService 사용)
        firebasePromptService.getPromptById(requestDTO, new DataCallback() {
            @Override
            public void onSuccess(String prompt, String name) {
                // Firebase에서 캐릭터 참조 이미지 제거 (FirebaseCharacterService 사용)
                firebaseCharacterService.clearCharacterReferenceImage(requestDTO);

                // 재생성 프로세스 시작
                SubmitResultVO result = processRegenerate(requestDTO, prompt, name);
                if (result.getCode() == ReturnCode.SUCCESS) {
                    callback.onSuccess(prompt, name); // 성공 시 콜백 호출
                } else {
                    callback.onFailure("Regenerate process failed.");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                // 'Data not found' 오류 발생 시 404로 처리
                if ("Data not found".equals(errorMessage)) {
                    callback.onFailure("404"); // 404로 표시
                } else {
                    callback.onFailure("Failed to get prompt: " + errorMessage);
                }
            }
        });
    }

    private SubmitResultVO processRegenerate(ReImagineRequestDTO requestDTO, String prompt, String name) {
        taskStatusService.setPromptCount(1);
        SubmitResultVO result = apiService.submitImagineByPrompt(prompt);

        CommonPromptDTO promptDTO = new CommonPromptDTO(requestDTO.getPromptType(), name, prompt);
        String studentTaskId = String.valueOf(requestDTO.getStudentTaskId());
        String imageId = String.valueOf(requestDTO.getImageId());

        if (result.getCode() == ReturnCode.SUCCESS) {
            taskStatusService.scheduleTaskStatusFetching(
                    false, TaskAction.IMAGINE, (String) result.getResult(),
                    bgRemovedImageUrl -> handleNonProtagonistComplete(result, promptDTO, studentTaskId, imageId),
                    promptDTO, studentTaskId, imageId
            );
        } else {
            // firebasePromptService로 변경
            firebasePromptService.updatePromptStatus(studentTaskId, promptDTO, prompt, "failed", "Failed to start imagine task");
        }
        return result;
    }


}
