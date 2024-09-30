package com.dingdong.imageserver.dto.request;

import com.dingdong.imageserver.enums.PromptType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class ReImagineRequestDTO implements Serializable {

    @NotNull
    private long studentTaskId;

    private long fairytaleId;

    @NotNull
    private String imageId;

    @NotNull
    private PromptType promptType;
}
