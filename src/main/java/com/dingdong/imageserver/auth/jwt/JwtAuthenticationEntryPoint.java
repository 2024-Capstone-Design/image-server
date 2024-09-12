package com.dingdong.imageserver.auth.jwt;

import com.dingdong.imageserver.exception.CustomException;
import com.dingdong.imageserver.response.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) {
        throw new CustomException(ErrorStatus.INVALID_TOKEN, "JwtAuthenticationEntryPoint: no valid credentials");
    }
}
