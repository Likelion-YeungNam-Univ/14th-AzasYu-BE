package com.syncbridge.app.domain.project.dto;

/**
 * 프로젝트 진행 상태.
 *
 * <p>project 테이블에는 상태 컬럼이 없으므로, 소속 회의(meeting)의 상태로부터 파생한다. 회의가 하나 이상 있고 전부 COMPLETED 이면 COMPLETED,
 * 그 외에는 IN_PROGRESS.
 */
public enum ProjectStatus {
  ALL,
  IN_PROGRESS,
  COMPLETED
}
