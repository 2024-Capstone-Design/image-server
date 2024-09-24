package com.dingdong.imageserver.controller;

import com.dingdong.imageserver.dto.ImagineRequestDTO;
import com.dingdong.imageserver.dto.ImagineStatusDTO;
import com.dingdong.imageserver.dto.ImagineTaskStatusDTO;
import com.dingdong.imageserver.dto.ReImagineRequestDTO;
import com.dingdong.imageserver.dto.style.CategoryDTO;
import com.dingdong.imageserver.model.DataCallback;
import com.dingdong.imageserver.response.ApiResponseDTO;
import com.dingdong.imageserver.response.ErrorStatus;
import com.dingdong.imageserver.response.SuccessStatus;
import com.dingdong.imageserver.service.StyleService;
import com.dingdong.imageserver.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;

@RestController
@RequestMapping("/api/v1/styles")
@RequiredArgsConstructor
public class StyleController {

    private final StyleService styleService;

    @GetMapping
    public List<CategoryDTO> getAllCategories() {
        return styleService.getAllCategories();
    }
}
