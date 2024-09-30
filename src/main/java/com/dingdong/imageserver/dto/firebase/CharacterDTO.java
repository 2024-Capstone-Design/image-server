package com.dingdong.imageserver.dto.firebase;

import lombok.Data;

@Data
public class CharacterDTO {
    private String name;
    private String prompt;
    private boolean isMain;
}
