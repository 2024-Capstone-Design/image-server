package com.dingdong.imageserver.dto;

import com.dingdong.imageserver.enums.TaskAction;
import lombok.Data;

import java.io.Serializable;

@Data
public class SubmitChangeDTO implements Serializable {
    private String taskId;
    private TaskAction action;
    private int index;
    private String state;
    private String notifyHook;
}
