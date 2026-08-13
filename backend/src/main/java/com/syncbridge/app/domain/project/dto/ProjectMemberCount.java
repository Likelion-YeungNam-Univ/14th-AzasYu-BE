package com.syncbridge.app.domain.project.dto;

/** 프로젝트별 멤버 수 집계 결과 (N+1 방지용 프로젝션). */
public record ProjectMemberCount(Long projectId, Long memberCount) {}
