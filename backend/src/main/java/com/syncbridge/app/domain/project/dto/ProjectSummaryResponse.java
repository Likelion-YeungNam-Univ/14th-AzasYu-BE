package com.syncbridge.app.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "내 프로젝트 목록 항목")
public record ProjectSummaryResponse(
    @Schema(description = "프로젝트 ID", example = "10") Long projectId,
    @Schema(description = "프로젝트 이름", example = "신규 서비스 기획") String name,
    @Schema(description = "프로젝트 설명", example = "AI 기반 협업 서비스 기획 프로젝트") String description,
    @Schema(description = "대표 색상", example = "#4A90E2") String color,
    @Schema(description = "참여 인원 수", example = "6") long memberCount,
    @Schema(description = "대표 멤버(생성자) 이름", example = "이지혜") String representativeMemberName,
    @Schema(description = "진행 상태", example = "IN_PROGRESS") ProjectStatus status,
    @Schema(description = "최근 활동 시각", example = "2026-08-12T14:00:00") LocalDateTime updatedAt) {}
