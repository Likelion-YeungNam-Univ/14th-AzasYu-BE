package com.syncbridge.app.global.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** 회의록 AI 분석(202 Accepted 후 백그라운드 처리)용 Executor. */
@Configuration
public class AsyncConfig {

  public static final String ANALYSIS_EXECUTOR = "analysisExecutor";

  @Bean(ANALYSIS_EXECUTOR)
  public Executor analysisExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("ai-analysis-");
    executor.initialize();
    return executor;
  }
}
