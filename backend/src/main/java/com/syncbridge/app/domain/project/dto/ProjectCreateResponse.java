package com.syncbridge.app.domain.project.dto;

import com.syncbridge.app.domain.project.entity.Project;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 생성 응답")
public record ProjectCreateResponse(
    @Schema(description = "프로젝트 ID", example = "10") Long projectId,
    @Schema(description = "프로젝트 이름", example = "신규 서비스 기획") String name,
    @Schema(description = "참여 코드", example = "A7K9-M2P4") String joinCode) {

  public static ProjectCreateResponse from(Project project) {
    return new ProjectCreateResponse(project.getId(), project.getName(), project.getJoinCode());
  }
}
