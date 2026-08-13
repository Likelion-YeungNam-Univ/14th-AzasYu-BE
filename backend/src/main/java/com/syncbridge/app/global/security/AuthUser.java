package com.syncbridge.app.global.security;

/**
 * SecurityContext 에 저장되는 인증 주체.
 *
 * <p>컨트롤러에서 {@code @AuthenticationPrincipal AuthUser authUser} 로 주입받아 사용한다.
 */
public record AuthUser(Long userId, String email, String name) {}
