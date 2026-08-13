package com.syncbridge.app.domain.project.controller;

import com.syncbridge.app.domain.project.dto.JoinProjectRequest;
import com.syncbridge.app.domain.project.dto.JoinProjectResponse;
import com.syncbridge.app.domain.project.dto.ProjectCreateRequest;
import com.syncbridge.app.domain.project.dto.ProjectCreateResponse;
import com.syncbridge.app.domain.project.dto.ProjectStatus;
import com.syncbridge.app.domain.project.dto.ProjectSummaryResponse;
import com.syncbridge.app.domain.project.service.ProjectService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "02. Project", description = "프로젝트 생성 / 목록 조회 / 참여 코드 합류")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

  private final ProjectService projectService;

  @Operation(
      summary = "내 프로젝트 목록 조회",
      description =
          "로그인 사용자가 속한 프로젝트 목록을 조회한다. status 로 진행 상태를 필터링한다. "
              + "(프로젝트 상태는 소속 회의 상태로부터 파생: 회의가 모두 COMPLETED 이면 COMPLETED)")
  @ApiResponse(responseCode = "200", description = "조회 성공")
  @GetMapping
  public ResponseEntity<List<ProjectSummaryResponse>> getMyProjects(
      @AuthenticationPrincipal AuthUser authUser,
      @Parameter(description = "조회할 상태 필터", example = "ALL")
          @RequestParam(defaultValue = "ALL")
          ProjectStatus status) {
    return ResponseEntity.ok(projectService.getMyProjects(authUser.userId(), status));
  }

  @Operation(
      summary = "새 프로젝트 생성",
      description = "프로젝트를 생성하고 참여 코드를 발급한다. 생성자는 자동으로 멤버가 되며, 이미 가입된 초대 이메일은 즉시 멤버로 등록된다.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "생성 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "입력값 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping
  public ResponseEntity<ProjectCreateResponse> createProject(
      @AuthenticationPrincipal AuthUser authUser,
      @Valid @RequestBody ProjectCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(projectService.createProject(authUser.userId(), request));
  }

  @Operation(summary = "참여 코드로 프로젝트 참여", description = "발급된 참여 코드(A7K9-M2P4 형식)로 프로젝트에 합류한다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "참여 성공"),
    @ApiResponse(
        responseCode = "404",
        description = "유효하지 않은 참여 코드(PROJECT_002)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "이미 참여 중(PROJECT_003)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping("/join")
  public ResponseEntity<JoinProjectResponse> joinProject(
      @AuthenticationPrincipal AuthUser authUser, @Valid @RequestBody JoinProjectRequest request) {
    return ResponseEntity.ok(projectService.joinByCode(authUser.userId(), request));
  }
}
