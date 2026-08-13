package com.syncbridge.app.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/** FastAPI AI 마이크로서비스 호출용 Reactive WebClient. */
@Configuration
public class WebClientConfig {

  /** 회의록 원문/분석 응답이 크므로 in-memory 버퍼를 넉넉히 확보한다. */
  private static final int MAX_IN_MEMORY_SIZE = 16 * 1024 * 1024;

  @Bean
  public WebClient aiServiceWebClient(
      @Value("${ai-service.url}") String aiServiceUrl,
      @Value("${ai-service.timeout-seconds:180}") long timeoutSeconds) {

    HttpClient httpClient =
        HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
            // 스트리밍 응답이 끊기지 않도록 response timeout 은 설정하지 않고
            // idle read timeout 만 적용한다.
            .doOnConnected(
                conn ->
                    conn.addHandlerLast(
                        new ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS)))
            .responseTimeout(Duration.ofSeconds(timeoutSeconds));

    return WebClient.builder()
        .baseUrl(aiServiceUrl)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
        .build();
  }
}
