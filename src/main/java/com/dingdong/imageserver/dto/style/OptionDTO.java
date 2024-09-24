package com.dingdong.imageserver.dto.style;

import lombok.Data;

@Data
public class OptionDTO {

    private Long id;
    private String name;

    public OptionDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

}
