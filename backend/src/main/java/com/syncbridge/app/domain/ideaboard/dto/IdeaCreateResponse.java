package com.syncbridge.app.domain.ideaboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아이디어 작성 응답")
public record IdeaCreateResponse(
    @Schema(description = "아이디어 ID", example = "503") Long ideaId,
    @Schema(description = "처리 결과", example = "SUCCESS") String status) {

  public static IdeaCreateResponse success(Long ideaId) {
    return new IdeaCreateResponse(ideaId, "SUCCESS");
  }
}
