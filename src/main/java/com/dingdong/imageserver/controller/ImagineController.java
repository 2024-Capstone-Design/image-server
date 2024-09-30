package com.dingdong.imageserver.controller;

import com.dingdong.imageserver.dto.firebase.ImagineStatusDTO;
import com.dingdong.imageserver.dto.firebase.ImagineTaskStatusDTO;
import com.dingdong.imageserver.dto.request.ImagineRequestDTO;
import com.dingdong.imageserver.dto.request.ReImagineRequestDTO;
import com.dingdong.imageserver.dto.DataCallback;
import com.dingdong.imageserver.response.ApiResponseDTO;
import com.dingdong.imageserver.response.ErrorStatus;
import com.dingdong.imageserver.response.SuccessStatus;
import com.dingdong.imageserver.service.imagine.ImagineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@Slf4j
@RequestMapping("/api/v1/imagine")
@RequiredArgsConstructor
public class ImagineController {
    private final ImagineService imagineService;

    /**
     * 동화 캐릭터, 배경 이미지 생성 Flow
     * @param requestDTO
     * @return
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponseDTO<String>> imagine(@RequestBody ImagineRequestDTO requestDTO) {
        imagineService.generateCharactersAndBackgrounds(requestDTO);
        return ApiResponseDTO.success(SuccessStatus.IMAGINE_REQUEST_SUCCESS);
    }

    /**
     * 이미지 재생성
     * @param requestDTO
     * @return
     */

    @PostMapping("/regenerate")
    public DeferredResult<ResponseEntity<ApiResponseDTO<String>>> regenerate(@RequestBody ReImagineRequestDTO requestDTO) {
        DeferredResult<ResponseEntity<ApiResponseDTO<String>>> deferredResult = new DeferredResult<>();

        imagineService.regenerate(requestDTO, new DataCallback() {
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

    /**
     * 이미지 재생성 상태 조회 with imageId
     * @param requestDTO
     * @return
     */
    @GetMapping("/regenerate/status")
    public ResponseEntity<ApiResponseDTO<ImagineStatusDTO>> getImagineStatusWithImageId(@RequestBody ReImagineRequestDTO requestDTO) {
        ImagineStatusDTO response = imagineService.getImagineStatusWithImageId(requestDTO);
        return ApiResponseDTO.success(SuccessStatus.REIMAGINE_STATUS_SUCCESS, response);
    }

    /**
     * 이미지 재생성 상태 조회 with studentTaskId
     * @param studentTaskId
     * @return
     */
    @GetMapping("/status/{studentTaskId}")
    public ResponseEntity<ApiResponseDTO<ImagineTaskStatusDTO>> getImagineStatus(@PathVariable String studentTaskId) {
        ImagineTaskStatusDTO response = imagineService.getImagineStatus(studentTaskId);
        return ApiResponseDTO.success(SuccessStatus.IMAGINE_STATUS_SUCCESS, response);
    }
}
