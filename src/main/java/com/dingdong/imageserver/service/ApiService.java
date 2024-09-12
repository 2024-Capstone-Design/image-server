package com.dingdong.imageserver.service;

import com.dingdong.imageserver.config.AppConfig;
import com.dingdong.imageserver.dto.SubmitChangeDTO;
import com.dingdong.imageserver.dto.SubmitImagineDTO;
import com.dingdong.imageserver.dto.SubmitResultVO;
import com.dingdong.imageserver.dto.prompt.CommonPromptDTO;
import com.dingdong.imageserver.model.Task;
import com.dingdong.imageserver.prompt.PromptRepository;
import com.dingdong.imageserver.prompt.Prompt;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ApiService {

    private final RestTemplate restTemplate;
    private final PromptRepository promptRepository;
    private final AppConfig config;

    public String getPromptValueFromDatabase(String key) {
        Prompt prompt = promptRepository.findByKey(key);
        return prompt != null ? prompt.getValue() : "";
    }

    public SubmitResultVO submitImagineByPrompt(String prompt) {
        SubmitImagineDTO imagineDTO = new SubmitImagineDTO();
        imagineDTO.setPrompt(prompt);
        return restTemplate.postForObject(config.getMjUrl() + "/submit/imagine", imagineDTO, SubmitResultVO.class);
    }

    public SubmitResultVO changeTask(SubmitChangeDTO changeDTO) {
        return restTemplate.postForObject(config.getMjUrl() + "/submit/change", changeDTO, SubmitResultVO.class);
    }

    public Task fetchAndSaveTask(String id) {
        // 프록시 서버 API를 호출하여 TaskDTO 데이터 가져오기
        Task task = restTemplate.getForObject(config.getMjUrl() + "/task/" + id + "/fetch", Task.class);

        return task;
    }

    public List<String> getPostProcessingImageUrl(String imageUrl, String studentTaskId, CommonPromptDTO promptDTO) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = createImagePostProcessingRequest(imageUrl, studentTaskId, promptDTO);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<List<Map<String, List<String>>>> response = restTemplate.exchange(
                    config.getBgRemoveUrl() + "/process_images/",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {});

            return extractImageUrlsFromResponse(response, promptDTO.getName());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error during background removal: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private Map<String, Object> createImagePostProcessingRequest(String imageUrl, String studentTaskId, CommonPromptDTO promptDTO) {
        Map<String, Object> request = new HashMap<>();
        request.put("studentTaskId", studentTaskId);
        request.put("imageRequest", Collections.singletonList(Map.of("name", promptDTO.getName(), "imageUrl", imageUrl, "promptType", promptDTO.getPromptType())));
        return request;
    }

    private List<String> extractImageUrlsFromResponse(ResponseEntity<List<Map<String, List<String>>>> response, String characterName) {
        if (response.getStatusCode() == HttpStatus.OK && response.hasBody()) {
            Map<String, List<String>> characterUrlMap = response.getBody().get(0);
            return characterUrlMap.getOrDefault(characterName, Collections.emptyList());
        } else {
            System.out.println("Failed to get background removed image URLs: " + response.getStatusCode());
            return Collections.emptyList();
        }
    }
}
