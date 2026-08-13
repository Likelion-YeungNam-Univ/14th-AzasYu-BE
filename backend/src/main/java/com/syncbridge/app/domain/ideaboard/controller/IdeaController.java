package com.syncbridge.app.domain.ideaboard.controller;

import com.syncbridge.app.domain.ideaboard.dto.IdeaCreateRequest;
import com.syncbridge.app.domain.ideaboard.dto.IdeaCreateResponse;
import com.syncbridge.app.domain.ideaboard.dto.IdeaResponseDto;
import com.syncbridge.app.domain.ideaboard.service.IdeaService;
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
import java.util.List;
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

@Tag(name = "05. Idea Board", description = "소프트 익명 아이디어 보드")
@RestController
@RequestMapping("/api/v1/meetings/{meetingId}/ideas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class IdeaController {

  private final IdeaService ideaService;

  @Operation(
      summary = "아이디어 보드 조회 (소프트 익명성)",
      description =
          "작성자 user_id 는 DB 에 저장되지만 응답에서는 '익명'으로 마스킹된다. AI 가 생성한 리스크 카드는 'AI 챗봇'으로 표시된다.")
  @ApiResponse(responseCode = "200", description = "조회 성공")
  @GetMapping
  public ResponseEntity<List<IdeaResponseDto>> getIdeas(
      @AuthenticationPrincipal AuthUser authUser,
      @Parameter(description = "회의 ID", example = "100") @PathVariable Long meetingId) {
    return ResponseEntity.ok(ideaService.getIdeas(meetingId, authUser.userId()));
  }

  @Operation(summary = "아이디어 작성", description = "익명으로 아이디어를 등록한다. 응답에는 작성자 정보가 포함되지 않는다.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "작성 성공"),
    @ApiResponse(
        responseCode = "403",
        description = "프로젝트 멤버가 아님(PROJECT_004)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping
  public ResponseEntity<IdeaCreateResponse> createIdea(
      @AuthenticationPrincipal AuthUser authUser,
      @Parameter(description = "회의 ID", example = "100") @PathVariable Long meetingId,
      @Valid @RequestBody IdeaCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ideaService.createIdea(meetingId, authUser.userId(), request));
  }
}
