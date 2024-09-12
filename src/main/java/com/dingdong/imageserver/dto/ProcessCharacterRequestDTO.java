package com.dingdong.imageserver.dto;

import com.dingdong.imageserver.dto.prompt.CommonPromptDTO;
import lombok.Data;

@Data
public class ProcessCharacterRequestDTO {

    private String referenceImage;
    private CommonPromptDTO character;
    private String studentTaskId;

}

