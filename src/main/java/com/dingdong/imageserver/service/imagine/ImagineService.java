package com.dingdong.imageserver.service.imagine;

import com.dingdong.imageserver.dto.request.ImagineRequestDTO;
import com.dingdong.imageserver.dto.firebase.ImagineStatusDTO;
import com.dingdong.imageserver.dto.firebase.ImagineTaskStatusDTO;
import com.dingdong.imageserver.dto.request.ReImagineRequestDTO;
import com.dingdong.imageserver.dto.service.CommonImageGenerationDTO;
import com.dingdong.imageserver.enums.PromptType;
import com.dingdong.imageserver.exception.CustomException;
import com.dingdong.imageserver.dto.DataCallback;
import com.dingdong.imageserver.response.ErrorStatus;
import com.dingdong.imageserver.service.firebase.FirebaseCharacterService;
import com.dingdong.imageserver.service.firebase.FirebasePromptService;
import com.dingdong.imageserver.service.firebase.FirebaseStatusService;
import com.dingdong.imageserver.service.firebase.FirebaseTaskService;
import com.dingdong.imageserver.utils.GenerationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final MidjourneyApiService midjourneyApiService;
    private final FirebaseTaskService firebaseTaskService;
    private final FirebasePromptService firebasePromptService;
    private final FirebaseStatusService firebaseStatusService;
    private final FirebaseCharacterService firebaseCharacterService;

    /**
     * 메인 캐릭터가 생성된 후 다른 캐릭터들과 배경을 한 번에 생성하는 서비스
     * @param requestDTO Imagine 요청 데이터
     */
    public void generateCharactersAndBackgrounds(ImagineRequestDTO requestDTO) {

        // 0. StudentTaskId/{path} 경로로 Firebase 작업 비동기 처리
        firebaseTaskService.initializeTaskInFirebase(requestDTO.getStudentTaskId(), success -> {
            if (success) {

                // 1. 먼저 주인공 캐릭터 생성 (참조될 캐릭터)
                CommonImageGenerationDTO mainCharacter = generationUtils.findProtagonist(requestDTO.getCharacters());
                if (mainCharacter == null) {
                    throw new IllegalArgumentException("Main character not found.");
                }

                // 주인공 캐릭터 생성
                convertPrompt(mainCharacter, null);
                String imageUrl = midjourneyTaskService.processTask(requestDTO.getStudentTaskId(), requestDTO.getFairytaleId(), mainCharacter);
                log.info("ref 이미지 생성 완료 "+ imageUrl);

                // 2. 주인공 생성 후 비동기로 다른 캐릭터 및 배경 생성
                CompletableFuture.runAsync(() -> {
                    createRemainingCharactersAndBackgrounds(imageUrl, requestDTO);
                });

            } else {
                // 실패 처리
            }
        });


    }

    private void createRemainingCharactersAndBackgrounds(String refImageUrl, ImagineRequestDTO requestDTO) {
        List<CommonImageGenerationDTO> nonProtagonistPrompts = generationUtils.findNonProtagonists(requestDTO.getCharacters());
        List<CommonImageGenerationDTO> backgroundPrompts = generationUtils.findBackgrounds(requestDTO.getBackgrounds());

        // 비동기 작업 리스트
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        generateListImages(refImageUrl, requestDTO, nonProtagonistPrompts, futures);
        generateListImages(refImageUrl, requestDTO, backgroundPrompts, futures);

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        System.out.println("모든 비동기 작업이 완료되었습니다.");
        firebaseTaskService.finalizeTask(requestDTO.getStudentTaskId());
    }

    private void generateListImages(String refImageUrl, ImagineRequestDTO requestDTO, List<CommonImageGenerationDTO> nonProtagonistPrompts, List<CompletableFuture<Void>> futures) {
        nonProtagonistPrompts.forEach(imageGenerationDTO -> {
            log.info("생성 "+ imageGenerationDTO);
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                convertPrompt(imageGenerationDTO, refImageUrl);
                midjourneyTaskService.processTask(requestDTO.getStudentTaskId(), requestDTO.getFairytaleId(), imageGenerationDTO);
            });
            futures.add(future);
        });
    }

    private void convertPrompt(CommonImageGenerationDTO promptDTO, String referenceImage) {
        StringBuilder prompt = new StringBuilder();
        if (promptDTO.getPromptType().equals(PromptType.CHARACTER)) {
            prompt.append(midjourneyApiService.getPromptValueFromDatabase("character-content"))
                    .append(promptDTO.getPrompt())
                    .append(midjourneyApiService.getPromptValueFromDatabase("character-style"))
                    .append(midjourneyApiService.getPromptValueFromDatabase("character-parameter"));
        } else {
            prompt.append(midjourneyApiService.getPromptValueFromDatabase("background-content"))
                    .append(promptDTO.getPrompt())
                    .append(midjourneyApiService.getPromptValueFromDatabase("background-style"))
                    .append(midjourneyApiService.getPromptValueFromDatabase("background-parameter"));
        }

        if (referenceImage != null){
            prompt.append(" " + midjourneyApiService.getPromptValueFromDatabase("character-sref")).append(referenceImage).toString();
        }

        promptDTO.setPrompt(String.valueOf(prompt));
    }


    public void regenerate(ReImagineRequestDTO requestDTO, DataCallback callback) {
        // Firebase에서 해당 prompt, name을 가져오는 작업 (FirebasePromptService 사용)
        firebasePromptService.getPromptById(requestDTO, new DataCallback() {
            @Override
            public void onSuccess(String prompt, String name) {
                // 비동기로 재생성 프로세스 실행
                CompletableFuture.runAsync(() -> {
                    // 캐릭터 참조 이미지 클리어
                    firebaseCharacterService.clearCharacterReferenceImage(requestDTO);
                    // 재생성 프로세스 시작
                    midjourneyTaskService.processTask(requestDTO.getStudentTaskId(), requestDTO.getFairytaleId(),
                            new CommonImageGenerationDTO(requestDTO.getPromptType(), name, prompt));
                    // Firebase에서 작업 완료 처리
                    firebaseTaskService.finalizeTask(requestDTO.getStudentTaskId());
                });

                // 프로세스가 완료되기 전에 callback 성공 반환
                callback.onSuccess(prompt, name);
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


}