package com.dingdong.imageserver.model;

public interface DataCallback {
    void onSuccess(String prompt, String name);
    void onFailure(String errorMessage);
}
