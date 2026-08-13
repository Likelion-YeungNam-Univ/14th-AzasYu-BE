package com.syncbridge.app.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponse(
    @Schema(description = "Access Token(JWT)", example = "eyJhbGciOi...") String accessToken,
    @Schema(description = "토큰 타입", example = "Bearer") String tokenType,
    @Schema(description = "만료 시간(초)", example = "86400") long expiresIn) {

  public static LoginResponse of(String accessToken, long expiresIn) {
    return new LoginResponse(accessToken, "Bearer", expiresIn);
  }
}
