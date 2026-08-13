package com.syncbridge.app.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** SPEC.md 2.x 에 정의된 errorCode 규격. */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // --- Auth ---
  PASSWORD_CONFIRM_MISMATCH("AUTH_001", "비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
  LOGIN_FAILED("AUTH_002", "없는 아이디이거나 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
  DUPLICATED_EMAIL("AUTH_003", "이미 가입된 이메일입니다.", HttpStatus.CONFLICT),
  UNAUTHORIZED("AUTH_004", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
  USER_NOT_FOUND("AUTH_005", "존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),

  // --- Project ---
  PROJECT_NOT_FOUND("PROJECT_001", "존재하지 않는 프로젝트입니다.", HttpStatus.NOT_FOUND),
  INVALID_JOIN_CODE("PROJECT_002", "유효하지 않은 참여 코드입니다.", HttpStatus.NOT_FOUND),
  ALREADY_JOINED_PROJECT("PROJECT_003", "이미 참여 중인 프로젝트입니다.", HttpStatus.CONFLICT),
  NOT_PROJECT_MEMBER("PROJECT_004", "해당 프로젝트에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN),

  // --- Meeting ---
  MEETING_NOT_FOUND("MEETING_001", "존재하지 않는 회의입니다.", HttpStatus.NOT_FOUND),
  NOT_MEETING_PARTICIPANT("MEETING_002", "해당 회의에 참여한 사용자가 아닙니다.", HttpStatus.FORBIDDEN),

  // --- Interview ---
  INVALID_QUESTION_NUM("INTERVIEW_001", "질문 번호는 1~6 사이여야 합니다.", HttpStatus.BAD_REQUEST),
  AI_SERVICE_ERROR("INTERVIEW_002", "AI 서비스 호출에 실패했습니다.", HttpStatus.BAD_GATEWAY),

  // --- Result / File ---
  MEETING_RESULT_NOT_FOUND("RESULT_001", "아직 생성된 회의 결과가 없습니다.", HttpStatus.NOT_FOUND),
  EMPTY_UPLOAD("RESULT_002", "회의록 파일 또는 텍스트 중 하나는 반드시 입력해야 합니다.", HttpStatus.BAD_REQUEST),
  UNSUPPORTED_FILE_TYPE("RESULT_003", "TXT, PDF, DOCX 파일만 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST),
  FILE_STORAGE_ERROR("RESULT_004", "파일 저장에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

  // --- Common ---
  INVALID_INPUT("COMMON_001", "입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
  INTERNAL_ERROR("COMMON_002", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
