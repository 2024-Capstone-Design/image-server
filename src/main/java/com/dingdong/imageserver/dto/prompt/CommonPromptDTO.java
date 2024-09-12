package com.dingdong.imageserver.dto.prompt;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommonPromptDTO {
    private PromptType promptType;
    private String name;
    private String prompt;
}
