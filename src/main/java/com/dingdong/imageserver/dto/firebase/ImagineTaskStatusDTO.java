package com.dingdong.imageserver.dto.firebase;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class ImagineTaskStatusDTO {
    private boolean completed;
    private boolean error;
    private String startTime;
    private String endTime;
    private List<ImagineStatusDTO> character;
    private List<ImagineStatusDTO> background;

    public ImagineTaskStatusDTO(boolean completed, boolean error, String startTime, String endTime, List<ImagineStatusDTO> character, List<ImagineStatusDTO> background) {
        this.completed = completed;
        this.error = error;
        this.startTime = startTime;
        this.endTime = endTime;
        this.character = character;
        this.background = background;
    }
}