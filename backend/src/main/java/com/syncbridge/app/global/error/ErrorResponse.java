package com.syncbridge.app.global.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 에러 응답")
public record ErrorResponse(
    @Schema(description = "에러 코드", example = "AUTH_001") String errorCode,
    @Schema(description = "에러 메시지", example = "비밀번호가 일치하지 않습니다.") String message) {

  public static ErrorResponse of(ErrorCode errorCode) {
    return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
  }

  public static ErrorResponse of(ErrorCode errorCode, String message) {
    return new ErrorResponse(errorCode.getCode(), message);
  }
}
