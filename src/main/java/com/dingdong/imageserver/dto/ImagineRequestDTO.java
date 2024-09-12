package com.dingdong.imageserver.dto;

import com.dingdong.imageserver.dto.prompt.BackgroundDTO;
import com.dingdong.imageserver.dto.prompt.CharacterDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ImagineRequestDTO implements Serializable {

    @NotNull
    private long studentTaskId;

    @NotNull
    private List<CharacterDTO> characters;

    @NotNull
    private List<BackgroundDTO> backgrounds;

    private String sketchImage;

}
