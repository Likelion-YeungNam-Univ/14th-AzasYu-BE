package com.syncbridge.app.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "사전 인터뷰 답변 제출 요청")
public record InterviewAnswerRequest(
    @Schema(description = "질문 번호(1~6)", example = "2")
        @Min(value = 1, message = "질문 번호는 1 이상이어야 합니다.")
        @Max(value = 6, message = "질문 번호는 6 이하여야 합니다.")
        int questionNum,
    @Schema(description = "AI 가 생성한 질문 원문", example = "이번에 기획하고 있는 서비스는 어떤 문제를 해결하기 위한 서비스인가요?")
        @NotBlank(message = "질문 원문은 필수입니다.")
        String questionText,
    @Schema(description = "사용자 답변", example = "정보가 너무 흩어져 있어서 불편한 문제를 해결하는 서비스라고 생각해요.")
        @NotBlank(message = "답변은 필수입니다.")
        String answerText) {}
