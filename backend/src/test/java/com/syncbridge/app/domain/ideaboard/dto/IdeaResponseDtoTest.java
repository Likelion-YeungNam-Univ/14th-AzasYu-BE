package com.syncbridge.app.domain.ideaboard.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.syncbridge.app.domain.auth.entity.User;
import com.syncbridge.app.domain.ideaboard.entity.IdeaCard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 소프트 익명성 매핑 규칙 검증. */
class IdeaResponseDtoTest {

  private static final User AUTHOR =
      User.builder().email("haeeon03@naver.com").passwordHash("hash").name("이지혜").build();

  @Test
  @DisplayName("팀원이 작성한 카드는 작성자가 '익명'으로 마스킹된다")
  void memberIdeaIsMaskedAsAnonymous() {
    IdeaCard card = IdeaCard.ofMember(null, AUTHOR, "추천 결과가 나온 이유를 함께 보여주면 좋겠어요.");

    IdeaResponseDto dto = IdeaResponseDto.from(card);

    assertThat(dto.authorName()).isEqualTo("익명");
    assertThat(dto.isAiGenerated()).isFalse();
    // DB 엔티티에는 작성자가 그대로 남아 있어야 한다 (개인 성과 반영 대비).
    assertThat(card.getUser()).isEqualTo(AUTHOR);
  }

  @Test
  @DisplayName("AI 가 생성한 카드는 작성자가 'AI 챗봇'으로 표시된다")
  void aiIdeaIsLabelledAsChatbot() {
    IdeaCard card = IdeaCard.ofAi(null, "[AI 리스크 감지] 추천 기준의 투명성에 대해 팀원 간 시각차가 존재합니다.");

    IdeaResponseDto dto = IdeaResponseDto.from(card);

    assertThat(dto.authorName()).isEqualTo("AI 챗봇");
    assertThat(dto.isAiGenerated()).isTrue();
  }

  @Test
  @DisplayName("공개 전환된 카드만 실명을 노출한다")
  void revealedIdeaExposesRealName() {
    IdeaCard card = IdeaCard.ofMember(null, AUTHOR, "간단한 구조였으면 좋겠어요.");
    card.reveal();

    assertThat(IdeaResponseDto.from(card).authorName()).isEqualTo("이지혜");
  }
}
