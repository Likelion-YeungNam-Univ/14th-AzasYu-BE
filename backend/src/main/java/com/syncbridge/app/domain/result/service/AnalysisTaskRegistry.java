package com.syncbridge.app.domain.result.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * 회의록 분석 작업 상태 저장소 (In-Memory).
 *
 * <p>업로드 API 가 202 Accepted 로 즉시 응답하므로 taskId 별 진행 상태를 보관한다. 운영 전환 시 Redis/DB 로 교체한다.
 */
@Component
public class AnalysisTaskRegistry {

  public enum TaskStatus {
    PROCESSING,
    COMPLETED,
    FAILED
  }

  private final AtomicLong sequence = new AtomicLong(9921);
  private final Map<String, TaskStatus> tasks = new ConcurrentHashMap<>();

  public String createTask() {
    String taskId = "TASK_" + sequence.incrementAndGet();
    tasks.put(taskId, TaskStatus.PROCESSING);
    return taskId;
  }

  public void markCompleted(String taskId) {
    tasks.put(taskId, TaskStatus.COMPLETED);
  }

  public void markFailed(String taskId) {
    tasks.put(taskId, TaskStatus.FAILED);
  }

  public TaskStatus getStatus(String taskId) {
    return tasks.get(taskId);
  }
}
