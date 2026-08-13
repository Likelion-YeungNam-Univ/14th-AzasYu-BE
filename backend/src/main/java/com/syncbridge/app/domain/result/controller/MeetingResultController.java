package com.syncbridge.app.domain.result.controller;

import com.syncbridge.app.domain.result.dto.MeetingResultResponse;
import com.syncbridge.app.domain.result.dto.MeetingUploadResponse;
import com.syncbridge.app.domain.result.service.MeetingResultService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "06. Meeting Result", description = "회의록 업로드 / AI 분석 리포트 조회")
@RestController
@RequestMapping("/api/v1/meetings/{meetingId}")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MeetingResultController {

  private final MeetingResultService meetingResultService;

  @Operation(
      summary = "회의록 파일/텍스트 업로드 및 AI 분석 요청",
      description =
          """
          TXT / PDF / DOCX 파일 또는 원문 텍스트를 업로드하면 202 Accepted 로 즉시 응답하고,
          백그라운드에서 FastAPI 분석 파이프라인을 실행한다.
          `file` 과 `rawText` 중 최소 하나는 반드시 있어야 한다.
          분석이 끝나면 `GET /api/v1/meetings/{meetingId}/result` 로 결과를 조회할 수 있다.
          """)
  @ApiResponses({
    @ApiResponse(responseCode = "202", description = "분석 시작"),
    @ApiResponse(
        responseCode = "400",
        description = "파일/텍스트 누락(RESULT_002) 또는 지원하지 않는 형식(RESULT_003)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<MeetingUploadResponse> uploadMeetingNote(
      @AuthenticationPrincipal AuthUser authUser,
      @Parameter(description = "회의 ID", example = "100") @PathVariable Long meetingId,
      @Parameter(description = "회의록 파일 (TXT/PDF/DOCX)")
          @RequestParam(value = "file", required = false)
          MultipartFile file,
      @Parameter(description = "회의록 원문 텍스트") @RequestParam(value = "rawText", required = false)
          String rawText) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(meetingResultService.requestAnalysis(meetingId, authUser.userId(), file, rawText));
  }

  @Operation(
      summary = "회의 결과 리포트 조회",
      description = "AI 가 구조화한 목적/주요 논의/결정 사항/실행 항목/오해 리스크를 조회한다. 분석 전이면 RESULT_001 을 반환한다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(
        responseCode = "404",
        description = "결과 없음(RESULT_001)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping("/result")
  public ResponseEntity<MeetingResultResponse> getResult(
      @AuthenticationPrincipal AuthUser authUser,
      @Parameter(description = "회의 ID", example = "100") @PathVariable Long meetingId) {
    return ResponseEntity.ok(meetingResultService.getResult(meetingId, authUser.userId()));
  }
}
