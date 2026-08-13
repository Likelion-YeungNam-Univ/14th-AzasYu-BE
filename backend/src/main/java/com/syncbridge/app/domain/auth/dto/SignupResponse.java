package com.syncbridge.app.domain.auth.dto;

import com.syncbridge.app.domain.auth.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답")
public record SignupResponse(
    @Schema(description = "사용자 ID", example = "1") Long userId,
    @Schema(description = "이메일", example = "haeeon03@naver.com") String email,
    @Schema(description = "이름", example = "이지혜") String name) {

  public static SignupResponse from(User user) {
    return new SignupResponse(user.getId(), user.getEmail(), user.getName());
  }
}
