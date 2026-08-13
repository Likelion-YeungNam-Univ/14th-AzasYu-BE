package com.syncbridge.app.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "회의 생성 요청")
public record MeetingCreateRequest(
    @Schema(description = "회의 제목", example = "6차 기획 회의")
        @NotBlank(message = "회의 제목은 필수입니다.")
        @Size(max = 100)
        String title,
    @Schema(description = "회의 목적", example = "새로운 서비스의 방향을 설정하고, 핵심 기능과 초기 아이디어를 구체화합니다.")
        @NotBlank(message = "회의 목적은 필수입니다.")
        String purpose,
    @Schema(
            description = "안건 목록(순서 유지)",
            example = "[\"1. 주요 기능 확정\", \"2. 타겟 논의\", \"3. 개발 일정 및 역할 분담\"]")
        List<String> agendas,
    @Schema(description = "회의 일시", example = "2026-08-12T14:00:00")
        @NotNull(message = "회의 일시는 필수입니다.")
        LocalDateTime meetingAt,
    @Schema(description = "예상 소요 시간(분)", example = "90") @Positive Integer durationMinutes,
    @Schema(description = "참석자 사용자 ID 목록", example = "[1, 2, 3, 4, 5, 6]")
        List<Long> participantUserIds) {}
