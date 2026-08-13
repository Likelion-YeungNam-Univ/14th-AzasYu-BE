package com.syncbridge.app.domain.result.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/** meeting_result.action_items JSONB 요소. */
@Schema(description = "실행 항목")
public record ActionItem(
    @Schema(description = "담당자", example = "이지혜") String assignee,
    @Schema(description = "할 일", example = "사용자 문제 및 핵심 타깃 정의") String task) {}
