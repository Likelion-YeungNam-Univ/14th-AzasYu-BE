package com.syncbridge.app.domain.interview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사전 인터뷰 답변 제출 응답")
public record InterviewAnswerResponse(
    @Schema(description = "다음 질문 번호(완료 시 null)", example = "3") Integer nextQuestionNum,
    // SPEC 2.4 의 JSON 키가 "isCompleted" 이므로 is-getter 관례에 의한 이름 변경을 막는다.
    @JsonProperty("isCompleted") @Schema(description = "인터뷰 완료 여부", example = "false")
        boolean isCompleted) {}
