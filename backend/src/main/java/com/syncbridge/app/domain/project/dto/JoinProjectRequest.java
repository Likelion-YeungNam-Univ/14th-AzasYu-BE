package com.syncbridge.app.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "참여 코드로 프로젝트 참여 요청")
public record JoinProjectRequest(
    @Schema(description = "참여 코드", example = "A7K9-M2P4")
        @NotBlank(message = "참여 코드는 필수입니다.")
        String joinCode) {}
