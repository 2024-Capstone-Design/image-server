package com.dingdong.imageserver.exception;

import com.dingdong.imageserver.response.ErrorStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomException extends RuntimeException {
    private final ErrorStatus errorStatus;
    private String value;
}