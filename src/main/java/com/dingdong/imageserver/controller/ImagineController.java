package com.dingdong.imageserver.controller;

import com.dingdong.imageserver.dto.*;
import com.dingdong.imageserver.model.DataCallback;
import com.dingdong.imageserver.response.ApiResponseDTO;
import com.dingdong.imageserver.response.ErrorStatus;
import com.dingdong.imageserver.response.SuccessStatus;
import com.dingdong.imageserver.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping("/api/v1/imagine")
@RequiredArgsConstructor
public class ImagineController {
    private final TaskService taskService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponseDTO<String>> imagine(@RequestBody ImagineRequestDTO requestDTO) {
        taskService.imagine(requestDTO);
        return ApiResponseDTO.success(SuccessStatus.IMAGINE_REQUEST_SUCCESS);
    }

    @PostMapping("/regenerate")
    public DeferredResult<ResponseEntity<ApiResponseDTO<String>>> regenerate(@RequestBody ReImagineRequestDTO requestDTO) {
        DeferredResult<ResponseEntity<ApiResponseDTO<String>>> deferredResult = new DeferredResult<>();

        taskService.regenerate(requestDTO, new DataCallback() {
            @Override
            public void onSuccess(String prompt, String name) {
                deferredResult.setResult(ApiResponseDTO.success(SuccessStatus.REIMAGINE_REQUEST_SUCCESS));
            }

            @Override
            public void onFailure(String errorMessage) {
                if ("404".equals(errorMessage)) {
                    deferredResult.setErrorResult(ApiResponseDTO.fail(ErrorStatus.ENTITY_NOT_FOUND, "Prompt data not found"));
                } else {
                    deferredResult.setErrorResult(ApiResponseDTO.fail(ErrorStatus.SERVER_ERROR, errorMessage));
                }
            }
        });

        return deferredResult;
    }

    @GetMapping("/regenerate/status")
    public ResponseEntity<ApiResponseDTO<ImagineStatusDTO>> getImagineStatusWithImageId(@RequestBody ReImagineRequestDTO requestDTO) {
        ImagineStatusDTO response = taskService.getImagineStatusWithImageId(requestDTO);
        return ApiResponseDTO.success(SuccessStatus.REIMAGINE_STATUS_SUCCESS, response);
    }

    @GetMapping("/status/{studentTaskId}")
    public ResponseEntity<ApiResponseDTO<ImagineTaskStatusDTO>> getImagineStatus(@PathVariable String studentTaskId) {
        ImagineTaskStatusDTO response = taskService.getImagineStatus(studentTaskId);
        return ApiResponseDTO.success(SuccessStatus.IMAGINE_STATUS_SUCCESS, response);
    }
}
