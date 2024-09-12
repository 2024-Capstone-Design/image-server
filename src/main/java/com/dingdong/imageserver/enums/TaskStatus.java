package com.dingdong.imageserver.enums;

import lombok.Getter;

public enum TaskStatus {
    /**
     * 시작되지 않음.
     */
    NOT_START(0),
    /**
     * 제출됨.
     */
    SUBMITTED(1),
    /**
     * 실행 중.
     */
    IN_PROGRESS(3),
    /**
     * 실패.
     */
    FAILURE(4),
    /**
     * 성공.
     */
    SUCCESS(4);

    @Getter
    private final int order;

    TaskStatus(int order) {
        this.order = order;
    }
}
