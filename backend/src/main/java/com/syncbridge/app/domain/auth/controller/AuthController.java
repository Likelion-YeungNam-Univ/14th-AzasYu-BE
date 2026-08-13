package com.syncbridge.app.domain.auth.controller;

import com.syncbridge.app.domain.auth.dto.LoginRequest;
import com.syncbridge.app.domain.auth.dto.LoginResponse;
import com.syncbridge.app.domain.auth.dto.SignupRequest;
import com.syncbridge.app.domain.auth.dto.SignupResponse;
import com.syncbridge.app.domain.auth.service.AuthService;
import com.syncbridge.app.global.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "01. Auth", description = "회원가입 / 로그인 / 로그아웃")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private static final String BEARER_PREFIX = "Bearer ";

  private final AuthService authService;

  @Operation(
      summary = "회원가입",
      description = "이메일/비밀번호/이름으로 회원가입한다. 비밀번호와 비밀번호 확인이 다르면 AUTH_001 에러를 반환한다.",
      security = {})
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "가입 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "비밀번호 불일치(AUTH_001) / 입력값 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "이미 가입된 이메일(AUTH_003)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping("/signup")
  public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
  }

  @Operation(
      summary = "로그인",
      description = "이메일/비밀번호로 로그인하고 JWT Access Token 을 발급받는다.",
      security = {})
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "로그인 성공"),
    @ApiResponse(
        responseCode = "401",
        description = "로그인 실패(AUTH_002)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @Operation(
      summary = "로그아웃",
      description = "현재 Access Token 을 서버 블랙리스트에 등록하여 무효화한다.",
      security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(responseCode = "204", description = "로그아웃 성공")
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
    if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
      authService.logout(authorization.substring(BEARER_PREFIX.length()).trim());
    }
    return ResponseEntity.noContent().build();
  }
}
