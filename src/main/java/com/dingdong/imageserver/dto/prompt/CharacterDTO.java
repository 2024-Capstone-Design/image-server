package com.dingdong.imageserver.dto.prompt;

import lombok.Data;

@Data
public class CharacterDTO {
    private String name;
    private String prompt;
    private boolean isMain;
}
