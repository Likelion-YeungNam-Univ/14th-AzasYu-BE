package com.syncbridge.app.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignupRequest(
    @Schema(description = "이메일", example = "haeeon03@naver.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,
    @Schema(description = "비밀번호", example = "password123!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        String password,
    @Schema(description = "비밀번호 확인", example = "password123!")
        @NotBlank(message = "비밀번호 확인은 필수입니다.")
        String passwordConfirm,
    @Schema(description = "이름", example = "이지혜")
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50)
        String name) {

  public boolean isPasswordMatched() {
    return password != null && password.equals(passwordConfirm);
  }
}
