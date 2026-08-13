package com.syncbridge.app.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  /** EventSource(SSE)는 커스텀 헤더를 붙일 수 없으므로 쿼리 파라미터 토큰을 함께 허용한다. */
  private static final String TOKEN_QUERY_PARAM = "token";

  private final JwtTokenProvider jwtTokenProvider;
  private final TokenBlacklist tokenBlacklist;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String token = resolveToken(request);
    if (StringUtils.hasText(token)
        && !tokenBlacklist.contains(token)
        && jwtTokenProvider.validateToken(token)) {
      SecurityContextHolder.getContext()
          .setAuthentication(jwtTokenProvider.getAuthentication(token));
    }
    filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    String bearer = request.getHeader(AUTHORIZATION_HEADER);
    if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)) {
      return bearer.substring(BEARER_PREFIX.length()).trim();
    }
    String queryToken = request.getParameter(TOKEN_QUERY_PARAM);
    return StringUtils.hasText(queryToken) ? queryToken.trim() : null;
  }
}
