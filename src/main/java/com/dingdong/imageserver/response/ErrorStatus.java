package com.dingdong.imageserver.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@AllArgsConstructor
public enum ErrorStatus {


    //400 Bad Request
    INVALID_REQUEST(BAD_REQUEST, "요청이 적절하지 않습니다."),

    //401 Unauthorized
    INVALID_TOKEN(UNAUTHORIZED, "토큰 정보가 유효하지 않습니다."),
    UNAUTHORIZED_INFO(UNAUTHORIZED, "인증에 실패했습니다."),

    //403 Forbidden
    INVALID_AUTHORITY(FORBIDDEN, "접근 권한이 없습니다."),

    //404 Not Found
    ENTITY_NOT_FOUND(NOT_FOUND, "해당하는 객체가 존재하지 않습니다."),

    //409 Conflict
    DUPLICATED_MEMBER(CONFLICT, "중복된 사용자입니다."),

    //500 Server Error
    SERVER_ERROR(INTERNAL_SERVER_ERROR, "서버 에러가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String detail;


}