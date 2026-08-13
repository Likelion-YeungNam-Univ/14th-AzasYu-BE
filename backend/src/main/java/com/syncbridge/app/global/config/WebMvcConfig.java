package com.syncbridge.app.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 프로토타이핑 테스트 클라이언트(`prototyping/index.html`)를 백엔드와 동일 오리진으로 서빙한다.
 *
 * <p>SPEC 4.3 의 테스트 코드는 `/api/v1/...` 상대 경로를 사용하므로, http://localhost:8080/prototyping/index.html 로
 * 접속하면 CORS 설정 없이 그대로 동작한다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/prototyping/**")
        .addResourceLocations(
            "file:../prototyping/", "file:./prototyping/", "classpath:/static/prototyping/");
  }
}
