package com.syncbridge.app.domain.meeting.controller;

import com.syncbridge.app.domain.meeting.dto.MeetingCreateRequest;
import com.syncbridge.app.domain.meeting.dto.MeetingCreateResponse;
import com.syncbridge.app.domain.meeting.dto.MeetingDetailResponse;
import com.syncbridge.app.domain.meeting.service.MeetingService;
import com.syncbridge.app.global.error.ErrorResponse;
import com.syncbridge.app.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "03. Meeting", description = "회의 생성 / 회의 상세 대시보드")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MeetingController {

  private final MeetingService meetingService;

  @Operation(
      summary = "회의 생성",
      description = "프로젝트 하위에 회의를 생성한다. 안건은 순서대로 저장되고, 참석자는 프로젝트 멤버여야 한다.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "생성 성공"),
    @ApiResponse(
        responseCode = "403",
        description = "프로젝트 멤버가 아님(PROJECT_004)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "프로젝트 없음(PROJECT_001)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping("/projects/{projectId}/meetings")
  public ResponseEntity<MeetingCreateResponse> createMeeting(
      @AuthenticationPrincipal AuthUser authUser,
      @Parameter(description = "프로젝트 ID", example = "10") @PathVariable Long projectId,
      @Valid @RequestBody MeetingCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(meetingService.createMeeting(projectId, authUser.userId(), request));
  }

  @Operation(
      summary = "회의 상세 대시보드 조회",
      description = "회의 기본 정보와 사전 인터뷰 진행 현황(완료 인원 / 전체 참석자)을 조회한다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(
        responseCode = "404",
        description = "회의 없음(MEETING_001)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping("/meetings/{meetingId}")
  public ResponseEntity<MeetingDetailResponse> getMeetingDetail(
      @AuthenticationPrincipal AuthUser authUser,
      @Parameter(description = "회의 ID", example = "100") @PathVariable Long meetingId) {
    return ResponseEntity.ok(meetingService.getMeetingDetail(meetingId, authUser.userId()));
  }
}
