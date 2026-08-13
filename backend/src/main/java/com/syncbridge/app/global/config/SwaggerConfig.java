package com.syncbridge.app.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Springdoc OpenAPI 2.x 설정. Swagger UI: http://localhost:8080/swagger-ui.html */
@Configuration
public class SwaggerConfig {

  private static final String BEARER_SCHEME = "bearerAuth";

  @Bean
  public OpenAPI syncBridgeOpenAPI() {
    Info info =
        new Info()
            .title("SyncBridge API")
            .version("v1")
            .description(
                """
                가짜합의(Fake Agreement) 방지 AI 협업 플랫폼 API 명세.

                - 인증: `POST /api/v1/auth/login` 으로 accessToken 발급 후 상단 Authorize 버튼에 입력
                - SSE 스트리밍(`.../interview/stream`)은 Swagger UI 에서 확인이 어려우므로
                  `prototyping/index.html` 테스트 클라이언트를 사용
                """);

    SecurityScheme bearerScheme =
        new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("로그인 응답의 accessToken 값을 입력하세요.");

    return new OpenAPI()
        .info(info)
        .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearerScheme))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
  }
}
