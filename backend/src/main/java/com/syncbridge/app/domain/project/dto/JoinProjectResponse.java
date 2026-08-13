package com.syncbridge.app.domain.project.dto;

import com.syncbridge.app.domain.project.entity.Project;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 참여 응답")
public record JoinProjectResponse(
    @Schema(description = "프로젝트 ID", example = "10") Long projectId,
    @Schema(description = "프로젝트 이름", example = "신규 서비스 기획") String name,
    @Schema(description = "안내 메시지", example = "프로젝트에 성공적으로 참여했습니다.") String message) {

  public static JoinProjectResponse from(Project project) {
    return new JoinProjectResponse(project.getId(), project.getName(), "프로젝트에 성공적으로 참여했습니다.");
  }
}
