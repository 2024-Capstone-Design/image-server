package com.dingdong.imageserver.dto.service;

import com.dingdong.imageserver.enums.PromptType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommonImageGenerationDTO {
    private PromptType promptType;
    private String name;
    private String prompt;
}
