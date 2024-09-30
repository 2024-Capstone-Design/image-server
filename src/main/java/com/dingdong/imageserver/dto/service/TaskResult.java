package com.dingdong.imageserver.dto.service;

public class TaskResult {
    private String taskId;
    private String imageId;

    public TaskResult(String taskId, String imageId) {
        this.taskId = taskId;
        this.imageId = imageId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getImageId() {
        return imageId;
    }
}

