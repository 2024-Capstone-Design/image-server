package com.dingdong.imageserver.dto;

import lombok.Data;

import java.util.List;

@Data
public class SubmitImagineDTO {

    private String prompt;
    private String base64;
    private List<String> base64Array;
    private String state;
    private String notifyHook;

}

