package com.dingdong.imageserver.service;

import com.dingdong.imageserver.dto.style.CategoryDTO;
import com.dingdong.imageserver.dto.style.OptionDTO;
import com.dingdong.imageserver.model.Category;
import com.dingdong.imageserver.model.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StyleService {

    private final CategoryRepository categoryRepository;

    public List<CategoryDTO> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(category -> new CategoryDTO(
                        category.getId(),
                        category.getName(),
                        category.getOptions().stream()
                                .map(option -> new OptionDTO(option.getId(), option.getName()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

}

