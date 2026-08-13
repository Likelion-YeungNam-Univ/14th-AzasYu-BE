package com.syncbridge.app.domain.result.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회의록 업로드 응답 (비동기 분석 시작)")
public record MeetingUploadResponse(
    @Schema(description = "분석 작업 ID", example = "TASK_9921") String taskId,
    @Schema(description = "작업 상태", example = "PROCESSING") String status,
    @Schema(description = "안내 메시지", example = "AI 분석이 시작되었습니다.") String message) {

  public static MeetingUploadResponse processing(String taskId) {
    return new MeetingUploadResponse(taskId, "PROCESSING", "AI 분석이 시작되었습니다.");
  }
}
