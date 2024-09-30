package com.dingdong.imageserver.dto.midjourney;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitResultVO implements Serializable {
    private int code;

    private String description;

    private String result;

    private Map<String, Object> properties = new HashMap<>();

}
