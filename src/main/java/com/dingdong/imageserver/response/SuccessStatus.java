package com.dingdong.imageserver.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.OK;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum SuccessStatus {

    /**
     * 인증
     */
    TOKEN_REFRESH_SUCCESS(HttpStatus.OK, "토큰 갱신 성공"),
    LOGOUT_SUCCESS(HttpStatus.OK, "로그아웃 성공"),

    /**
     * 이미지 생성
     */
    IMAGINE_REQUEST_SUCCESS(OK, "이미지 생성 요청 성공"),
    REIMAGINE_REQUEST_SUCCESS(OK, "이미지 재생성 요청 성공"),
    REIMAGINE_STATUS_SUCCESS(OK, "이미지 재생성 현황 조회 성공"),
    IMAGINE_STATUS_SUCCESS(OK, "이미지 생성 현황 조회 성공");

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}

