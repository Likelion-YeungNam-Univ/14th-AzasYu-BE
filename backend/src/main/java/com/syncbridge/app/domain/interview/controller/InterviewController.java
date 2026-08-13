package com.syncbridge.app.domain.interview.controller;

import com.syncbridge.app.domain.interview.dto.InterviewAnswerRequest;
import com.syncbridge.app.domain.interview.dto.InterviewAnswerResponse;
import com.syncbridge.app.domain.interview.service.InterviewService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Tag(name = "04. AI Pre-Interview", description = "AI 사전 인터뷰 (SSE 스트리밍 / 답변 제출)")
@RestController
@RequestMapping("/api/v1/meetings/{meetingId}/interview")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class InterviewController {

  private final InterviewService interviewService;

  @Operation(
      summary = "AI 사전 인터뷰 질문 스트리밍 (SSE)",
      description =
          """
          FastAPI(GPT-4o)가 생성하는 질문을 타자기 효과로 실시간 중계한다.

          - `Content-Type: text/event-stream`
          - 청크: `data: {"chunk": "이번에 "}`
          - 종료: `data: {"status": "DONE", "questionNum": 2}`
          - 실패: `data: {"status": "ERROR", "errorCode": "INTERVIEW_002", ...}` 직후 DONE

          브라우저 `EventSource` 는 커스텀 헤더를 보낼 수 없으므로, `Authorization` 헤더 대신
          `?token={accessToken}` 쿼리 파라미터로도 인증할 수 있다.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "스트리밍 시작",
        content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)),
    @ApiResponse(
        responseCode = "400",
        description = "질문 번호 범위 오류(INTERVIEW_001)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> streamQuestion(
      @AuthenticationPrincipal AuthUser authUser,
      @Parameter(description = "회의 ID", example = "100") @PathVariable Long meetingId,
      @Parameter(description = "질문 번호(1~6)", example = "2") @RequestParam(defaultValue = "1")
          int questionNum) {
    return interviewService.streamQuestion(meetingId, authUser.userId(), questionNum);
  }

  @Operation(summary = "사전 인터뷰 답변 제출", description = "질문/답변을 저장하고 다음 질문 번호를 반환한다. 6번 답변 시 인터뷰가 완료된다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "저장 성공"),
    @ApiResponse(
        responseCode = "404",
        description = "회의 없음(MEETING_001)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping("/answer")
  public ResponseEntity<InterviewAnswerResponse> submitAnswer(
      @AuthenticationPrincipal AuthUser authUser,
      @Parameter(description = "회의 ID", example = "100") @PathVariable Long meetingId,
      @Valid @RequestBody InterviewAnswerRequest request) {
    return ResponseEntity.ok(
        interviewService.submitAnswer(meetingId, authUser.userId(), request));
  }
}
