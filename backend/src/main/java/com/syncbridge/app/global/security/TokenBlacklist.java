package com.syncbridge.app.global.security;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 로그아웃된 Access Token 저장소.
 *
 * <p>Stateless JWT 구조이므로 만료 전 토큰을 무효화하려면 별도 저장소가 필요하다. 프로토타이핑 단계에서는 In-Memory 로 유지하고, 운영 전환 시 Redis
 * 구현체로 교체한다.
 */
@Component
public class TokenBlacklist {

  private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

  public void add(String token, Date expiration) {
    evictExpired();
    blacklist.put(token, expiration == null ? 0L : expiration.getTime());
  }

  public boolean contains(String token) {
    Long expiresAt = blacklist.get(token);
    if (expiresAt == null) {
      return false;
    }
    if (expiresAt < System.currentTimeMillis()) {
      blacklist.remove(token);
      return false;
    }
    return true;
  }

  private void evictExpired() {
    long now = System.currentTimeMillis();
    blacklist.entrySet().removeIf(entry -> entry.getValue() < now);
  }
}
