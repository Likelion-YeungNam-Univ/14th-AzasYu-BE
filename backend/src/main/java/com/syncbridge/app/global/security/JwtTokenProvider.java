package com.syncbridge.app.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/** JWT 발급 / 검증 / Authentication 변환. */
@Slf4j
@Component
public class JwtTokenProvider {

  private static final String CLAIM_EMAIL = "email";
  private static final String CLAIM_NAME = "name";

  private final SecretKey secretKey;
  private final long expirationSeconds;

  public JwtTokenProvider(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration-seconds:86400}") long expirationSeconds) {
    this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    this.expirationSeconds = expirationSeconds;
  }

  public long getExpirationSeconds() {
    return expirationSeconds;
  }

  public String createAccessToken(Long userId, String email, String name) {
    Date now = new Date();
    Date expiresAt = new Date(now.getTime() + expirationSeconds * 1000L);
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim(CLAIM_EMAIL, email)
        .claim(CLAIM_NAME, name)
        .issuedAt(now)
        .expiration(expiresAt)
        .signWith(secretKey)
        .compact();
  }

  public boolean validateToken(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      log.debug("Invalid JWT: {}", e.getMessage());
      return false;
    }
  }

  public Authentication getAuthentication(String token) {
    Claims claims = parseClaims(token);
    AuthUser principal =
        new AuthUser(
            Long.parseLong(claims.getSubject()),
            claims.get(CLAIM_EMAIL, String.class),
            claims.get(CLAIM_NAME, String.class));
    return new UsernamePasswordAuthenticationToken(
        principal, token, List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }

  public Date getExpiration(String token) {
    return parseClaims(token).getExpiration();
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
  }
}
