package com.syncbridge.app.domain.meeting.dto;

import com.syncbridge.app.domain.meeting.entity.MeetingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "회의 상세 대시보드")
public record MeetingDetailResponse(
    @Schema(description = "회의 ID", example = "100") Long meetingId,
    @Schema(description = "프로젝트 이름", example = "신규 서비스 기획") String projectName,
    @Schema(description = "참여 코드", example = "A7K9-M2P4") String joinCode,
    @Schema(description = "회의 제목", example = "6차 기획 회의") String title,
    @Schema(description = "회의 목적", example = "새로운 서비스의 방향을 설정합니다.") String purpose,
    @Schema(description = "안건 목록") List<String> agendas,
    @Schema(description = "회의 일시", example = "2026-08-12T14:00:00") LocalDateTime meetingAt,
    @Schema(description = "예상 소요 시간(분)", example = "90") Integer durationMinutes,
    @Schema(description = "사전 인터뷰 완료 인원", example = "4") long completedInterviewCount,
    @Schema(description = "전체 참석자 수", example = "6") long totalParticipantCount,
    @Schema(description = "회의 상태", example = "BEFORE_INTERVIEW") MeetingStatus status) {}
