package com.dingdong.imageserver.dto;

public interface DataCallback {
    void onSuccess(String prompt, String name);
    void onFailure(String errorMessage);
}
