package com.dingdong.imageserver.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ImagineStatusDTO {
    private String id;
    private String name;
    private String prompt;
    private String status;
    private String progress;
    private List<String> imageUrls;

    public ImagineStatusDTO(String id, String name, String prompt, String status, String progress, List<String> imageUrls) {
        this.id = id;
        this.name = name;
        this.prompt = prompt;
        this.status = status;
        this.progress = progress;
        this.imageUrls = imageUrls;
    }

}