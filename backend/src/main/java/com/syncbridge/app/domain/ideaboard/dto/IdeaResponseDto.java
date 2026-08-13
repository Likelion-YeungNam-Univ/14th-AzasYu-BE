package com.syncbridge.app.domain.ideaboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.syncbridge.app.domain.ideaboard.entity.IdeaCard;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 아이디어 보드 응답 DTO.
 *
 * <p><b>소프트 익명성 규칙</b>
 *
 * <ul>
 *   <li>AI 생성 카드 → {@code authorName = "AI 챗봇"}
 *   <li>팀원 작성 카드 → {@code authorName = "익명"} (DB 의 user_id 는 유지되지만 외부로 노출하지 않는다)
 *   <li>{@code isRevealed = true} 로 전환된 카드만 실명을 노출한다
 * </ul>
 */
@Schema(description = "아이디어 카드 (소프트 익명성 적용)")
public record IdeaResponseDto(
    @Schema(description = "아이디어 ID", example = "501") Long ideaId,
    @Schema(description = "내용", example = "추천 결과가 나온 이유를 함께 보여주면 좋겠어요.") String content,
    // SPEC 2.5 의 JSON 키가 "isAiGenerated" 이므로 is-getter 관례에 의한 이름 변경을 막는다.
    @JsonProperty("isAiGenerated") @Schema(description = "AI 생성 여부", example = "false")
        boolean isAiGenerated,
    @Schema(description = "작성자 표시명(익명 마스킹)", example = "익명") String authorName,
    @Schema(description = "작성 시각", example = "2026-08-12T11:00:00") LocalDateTime createdAt) {

  public static final String ANONYMOUS_NAME = "익명";
  public static final String AI_AUTHOR_NAME = "AI 챗봇";

  public static IdeaResponseDto from(IdeaCard ideaCard) {
    return new IdeaResponseDto(
        ideaCard.getId(),
        ideaCard.getContent(),
        ideaCard.isAiGenerated(),
        resolveAuthorName(ideaCard),
        ideaCard.getCreatedAt());
  }

  private static String resolveAuthorName(IdeaCard ideaCard) {
    if (ideaCard.isAiGenerated()) {
      return AI_AUTHOR_NAME;
    }
    if (ideaCard.isRevealed() && ideaCard.getUser() != null) {
      return ideaCard.getUser().getName();
    }
    return ANONYMOUS_NAME;
  }
}
