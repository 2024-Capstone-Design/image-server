package com.dingdong.imageserver.dto.request;

import com.dingdong.imageserver.dto.firebase.BackgroundDTO;
import com.dingdong.imageserver.dto.firebase.CharacterDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ImagineRequestDTO implements Serializable {

    @NotNull
    private long studentTaskId;

    @NotNull
    private long fairytaleId;

    @NotNull
    private List<CharacterDTO> characters;

    @NotNull
    private List<BackgroundDTO> backgrounds;

    @NotNull
    private List<Long> optionIds;


    private String sketchImage;
}
