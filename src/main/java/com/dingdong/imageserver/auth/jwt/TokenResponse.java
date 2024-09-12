package com.dingdong.imageserver.auth.jwt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TokenResponse {

  @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
  private String accessToken;

  @Schema(description = "리프레시 토큰", example = "dGhpc2lzYXJlZnJlc2h0b2tlbg==")
  private String refreshToken;

}
