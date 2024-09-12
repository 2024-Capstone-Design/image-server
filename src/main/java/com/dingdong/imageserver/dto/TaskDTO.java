package com.dingdong.imageserver.dto;

import com.dingdong.imageserver.enums.TaskAction;
import com.dingdong.imageserver.enums.TaskStatus;
import lombok.Data;

@Data
public class TaskDTO {
    private Long id;
    private TaskAction action;
    private TaskStatus status;
    private String prompt;
    private String promptEn;
    private String description;
    private String state;
    private Long submitTime;
    private Long startTime;
    private Long finishTime;
    private String imageUrl;
    private String progress;
    private String failReason;
}
