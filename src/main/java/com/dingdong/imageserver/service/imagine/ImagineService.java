package com.dingdong.imageserver.service.imagine;

import com.dingdong.imageserver.dto.request.ImagineRequestDTO;
import com.dingdong.imageserver.dto.firebase.ImagineStatusDTO;
import com.dingdong.imageserver.dto.firebase.ImagineTaskStatusDTO;
import com.dingdong.imageserver.dto.request.ReImagineRequestDTO;
import com.dingdong.imageserver.dto.service.CommonImageGenerationDTO;
import com.dingdong.imageserver.enums.PromptType;
import com.dingdong.imageserver.exception.CustomException;
import com.dingdong.imageserver.model.style.Option;
import com.dingdong.imageserver.model.style.OptionRepository;
import com.dingdong.imageserver.response.ErrorStatus;
import com.dingdong.imageserver.service.firebase.FirebaseUpdateService;
import com.dingdong.imageserver.service.firebase.FirebaseFetchService;
import com.dingdong.imageserver.utils.GenerationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 주인공과 등장인물의 이미지 생성 흐름을 관리하며, 전체 이미지 생성 프로세스를 관리
 *
 * 1. ImagineRequestDTO를 받아와서 이미지를 생성해야 할 캐릭터, 배경을 CommonImageGenerationDTO 리스트로 변환
 * 2. 현재 StudentTaskId와 연결된 Firebase 초기화 (initializeTaskInFirebase)
 * 3. 성공 시 TaskService를 통해 메인 캐릭터(참조용)의 이미지 url 반환
 * 4. 성공 시 다른 캐릭터와 배경을 TaskService를 수행
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ImagineService {

    private final GenerationUtils generationUtils;
    private final MidjourneyTaskService midjourneyTaskService;
    private final ThirdPartyIApiService thirdPartyIApiService;
    private final FirebaseUpdateService firebaseUpdateService;
    private final FirebaseFetchService firebaseFetchService;
    private final OptionRepository optionRepository;

    /**
     * 메인 캐릭터가 생성된 후 다른 캐릭터들과 배경을 한 번에 생성하는 서비스
     * @param requestDTO Imagine 요청 데이터
     */
    public void generateCharactersAndBackgrounds(ImagineRequestDTO requestDTO) {

        // 0. StudentTaskId/{path} 경로로 Firebase 작업 비동기 처리
        firebaseUpdateService.initializeTaskInFirebase(requestDTO.getStudentTaskId(), success -> {
            if (success) {
                // 1. 먼저 주인공 캐릭터 생성 (참조될 캐릭터)
                CommonImageGenerationDTO mainCharacter = generationUtils.findProtagonist(requestDTO.getCharacters());
                if (mainCharacter == null) {
                    throw new IllegalArgumentException("Main character not found.");
                }

                // 주인공 캐릭터 생성 비동기 호출
                convertPrompt(requestDTO.getStudentTaskId(), mainCharacter, null, requestDTO.getOptionIds());
                midjourneyTaskService.processTask(requestDTO.getStudentTaskId(), requestDTO.getFairytaleId(), mainCharacter)
                        .thenAccept(imageUrl -> {
                            log.info("ref 이미지 생성 완료 " + imageUrl);

                            // 2. 주인공 생성 후 다른 캐릭터 및 배경 생성
                            createRemainingCharactersAndBackgrounds(imageUrl, requestDTO);
                        });
            }
        });
    }

    void createRemainingCharactersAndBackgrounds(String refImageUrl, ImagineRequestDTO requestDTO) {
        List<CommonImageGenerationDTO> nonProtagonistPrompts = generationUtils.findNonProtagonists(requestDTO.getCharacters());
        List<CommonImageGenerationDTO> backgroundPrompts = generationUtils.findBackgrounds(requestDTO.getBackgrounds());

        // 비동기 작업 리스트
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        generateListImages(refImageUrl, requestDTO, nonProtagonistPrompts, futures);
        generateListImages(refImageUrl, requestDTO, backgroundPrompts, futures);

        // 모든 비동기 작업을 기다림
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            log.info("모든 비동기 작업이 완료되었습니다.");
            firebaseUpdateService.finalizeTask(requestDTO.getStudentTaskId());
        });

    }

    void generateListImages(String refImageUrl, ImagineRequestDTO requestDTO, List<CommonImageGenerationDTO> prompts, List<CompletableFuture<Void>> futures) {
        prompts.forEach(imageGenerationDTO -> {
            log.info("생성 " + imageGenerationDTO);
            convertPrompt(requestDTO.getStudentTaskId(), imageGenerationDTO, refImageUrl, null);
            CompletableFuture<Void> future = midjourneyTaskService.processTask(requestDTO.getStudentTaskId(), requestDTO.getFairytaleId(), imageGenerationDTO)
                    .thenAccept(imageUrl -> {
                        log.info(requestDTO.getStudentTaskId() + " " + requestDTO.getFairytaleId() + " 이미지 처리 완료: " + imageUrl);
                    });
            futures.add(future);
        });
    }

    private void convertPrompt(Long studentTaskId, CommonImageGenerationDTO promptDTO, String referenceImage, List<Long> optionIds) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(studentTaskId + " ");
        if (promptDTO.getPromptType().equals(PromptType.CHARACTER)) {
            prompt.append(thirdPartyIApiService.getPromptValueFromDatabase("character-content"))
                    .append(promptDTO.getPrompt());

            if (optionIds != null && ! optionIds.isEmpty()){
                List<Option> selectedOptions = optionRepository.findAllById(optionIds);
                for (Option option : selectedOptions) {
                    prompt.append(", ").append(option.getPrompt());
                }
            }

            prompt.append(thirdPartyIApiService.getPromptValueFromDatabase("character-style"))
                    .append(thirdPartyIApiService.getPromptValueFromDatabase("character-parameter"));

        } else {
            prompt.append(thirdPartyIApiService.getPromptValueFromDatabase("background-content"))
                    .append(promptDTO.getPrompt())
                    .append(thirdPartyIApiService.getPromptValueFromDatabase("background-style"))
                    .append(thirdPartyIApiService.getPromptValueFromDatabase("background-parameter"));
        }

        if (referenceImage != null){
            prompt.append(" " + thirdPartyIApiService.getPromptValueFromDatabase("character-sref")).append(referenceImage).toString();
        }

        promptDTO.setPrompt(String.valueOf(prompt));
    }

    @Async("taskExecutor")
    public void regenerate(ReImagineRequestDTO requestDTO) throws ExecutionException, InterruptedException, TimeoutException {
        ImagineStatusDTO imagineStatusDTO = firebaseFetchService.getImagineStatusWithImageId(requestDTO, 10);
        if (imagineStatusDTO != null){
            firebaseUpdateService.clearCharacterReferenceImage(requestDTO);
            midjourneyTaskService.processTask(requestDTO.getStudentTaskId(), requestDTO.getFairytaleId(),
                            new CommonImageGenerationDTO(requestDTO.getPromptType(), imagineStatusDTO.getName(), imagineStatusDTO.getPrompt()))
                    .thenAccept(imageUrl -> {
                        log.info("이미지 재생성 완료 " + imageUrl);
                        firebaseUpdateService.finalizeTask(requestDTO.getStudentTaskId());
                    });
        }
    }

    public ImagineTaskStatusDTO getImagineStatus(String studentTaskId) {
        try {
            return firebaseFetchService.getImagineStatusFromFirebase(studentTaskId, 10);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new CustomException(ErrorStatus.SERVER_ERROR, e.getMessage());
        }
    }

}