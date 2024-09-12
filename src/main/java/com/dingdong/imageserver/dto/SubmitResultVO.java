package com.dingdong.imageserver.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SubmitResultVO implements Serializable {
    private int code;
    private String message;
    private Object result;
//    private long estimatedTime;

    public static SubmitResultVO success(String message) {
        SubmitResultVO resultVO = new SubmitResultVO();
        resultVO.setCode(0); // 성공 코드
        resultVO.setMessage(message);
        return resultVO;
    }

    public static SubmitResultVO fail(int code, String message) {
        SubmitResultVO resultVO = new SubmitResultVO();
        resultVO.setCode(code);
        resultVO.setMessage(message);
        return resultVO;
    }

}
