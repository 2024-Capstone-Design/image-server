package com.dingdong.imageserver.controller;

import com.dingdong.imageserver.dto.style.CategoryDTO;
import com.dingdong.imageserver.service.StyleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
