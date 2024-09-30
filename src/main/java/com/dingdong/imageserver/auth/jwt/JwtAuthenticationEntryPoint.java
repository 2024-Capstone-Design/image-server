package com.dingdong.imageserver.auth.jwt;

import com.dingdong.imageserver.exception.CustomException;
import com.dingdong.imageserver.response.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.util.Enumeration;

@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) {

        // 요청 정보 로깅
        log.error("Unauthorized request. Method: {}, URI: {}, Remote Address: {}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr());

        authException.printStackTrace();
        log.error(authException.getMessage());

        // 헤더 정보 로깅
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                log.error("Unauthorized request. Method: {}, URI: {}, Remote Address: {}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
            }
        }

        throw new CustomException(ErrorStatus.INVALID_TOKEN, "JwtAuthenticationEntryPoint: no valid credentials");
    }
}
