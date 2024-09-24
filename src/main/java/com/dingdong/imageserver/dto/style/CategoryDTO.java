package com.dingdong.imageserver.dto.style;

import lombok.Data;

import java.util.List;

@Data
public class CategoryDTO {

    private Long id;
    private String name;
    private List<OptionDTO> options;

    public CategoryDTO(Long id, String name, List<OptionDTO> options) {
        this.id = id;
        this.name = name;
        this.options = options;
    }

}
