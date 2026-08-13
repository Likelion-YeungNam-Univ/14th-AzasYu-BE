package com.syncbridge.app.domain.auth.service;

import com.syncbridge.app.domain.auth.dto.LoginRequest;
import com.syncbridge.app.domain.auth.dto.LoginResponse;
import com.syncbridge.app.domain.auth.dto.SignupRequest;
import com.syncbridge.app.domain.auth.dto.SignupResponse;
import com.syncbridge.app.domain.auth.entity.User;
import com.syncbridge.app.domain.auth.repository.UserRepository;
import com.syncbridge.app.global.error.CustomException;
import com.syncbridge.app.global.error.ErrorCode;
import com.syncbridge.app.global.security.JwtTokenProvider;
import com.syncbridge.app.global.security.TokenBlacklist;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final TokenBlacklist tokenBlacklist;

  @Transactional
  public SignupResponse signup(SignupRequest request) {
    if (!request.isPasswordMatched()) {
      throw new CustomException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
    }
    if (userRepository.existsByEmail(request.email())) {
      throw new CustomException(ErrorCode.DUPLICATED_EMAIL);
    }

    User user =
        userRepository.save(
            User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .build());

    return SignupResponse.from(user);
  }

  public LoginResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new CustomException(ErrorCode.LOGIN_FAILED);
    }

    String accessToken =
        jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getName());
    return LoginResponse.of(accessToken, jwtTokenProvider.getExpirationSeconds());
  }

  /** Stateless JWT 이므로 서버 측 블랙리스트에 등록하여 만료 전 토큰을 무효화한다. */
  public void logout(String accessToken) {
    if (accessToken == null || accessToken.isBlank()) {
      return;
    }
    if (jwtTokenProvider.validateToken(accessToken)) {
      tokenBlacklist.add(accessToken, jwtTokenProvider.getExpiration(accessToken));
    }
  }
}
