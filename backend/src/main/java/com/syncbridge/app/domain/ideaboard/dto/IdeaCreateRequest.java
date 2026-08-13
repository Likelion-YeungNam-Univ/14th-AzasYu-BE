package com.syncbridge.app.domain.ideaboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "아이디어 작성 요청")
public record IdeaCreateRequest(
    @Schema(
            description = "아이디어 내용",
            example = "처음 사용하는 사람도 별도의 설명 없이 바로 이해할 수 있는 간단한 구조였으면 좋겠어요.")
        @NotBlank(message = "아이디어 내용은 필수입니다.")
        @Size(max = 2000, message = "아이디어는 2000자 이하로 작성해주세요.")
        String content) {}
