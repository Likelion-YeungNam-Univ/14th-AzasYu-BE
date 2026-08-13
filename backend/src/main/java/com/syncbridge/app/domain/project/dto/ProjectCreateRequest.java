package com.syncbridge.app.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "프로젝트 생성 요청")
public record ProjectCreateRequest(
    @Schema(description = "프로젝트 이름", example = "신규 서비스 기획")
        @NotBlank(message = "프로젝트 이름은 필수입니다.")
        @Size(max = 100)
        String name,
    @Schema(description = "프로젝트 설명", example = "가짜합의 방지를 위한 프로젝트") String description,
    @Schema(description = "대표 색상", example = "#4A90E2") String color,
    @Schema(
            description = "초대할 팀원 이메일 목록 (이미 가입된 사용자만 즉시 멤버로 등록됨)",
            example = "[\"park@naver.com\", \"lee@naver.com\"]")
        List<String> inviteEmails) {}
