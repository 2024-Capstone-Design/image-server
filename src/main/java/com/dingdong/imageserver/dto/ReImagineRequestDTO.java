package com.dingdong.imageserver.dto;

import com.dingdong.imageserver.dto.prompt.PromptType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class ReImagineRequestDTO implements Serializable {

    @NotNull
    private long studentTaskId;

    @NotNull
    private String imageId;

    @NotNull
    private PromptType promptType;
}
