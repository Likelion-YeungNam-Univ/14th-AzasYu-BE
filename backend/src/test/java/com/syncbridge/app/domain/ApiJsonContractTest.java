package com.syncbridge.app.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncbridge.app.domain.auth.entity.User;
import com.syncbridge.app.domain.ideaboard.dto.IdeaResponseDto;
import com.syncbridge.app.domain.ideaboard.entity.IdeaCard;
import com.syncbridge.app.domain.interview.dto.InterviewAnswerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SPEC 2.4 / 2.5 의 JSON 키 이름이 유지되는지 검증한다. */
class ApiJsonContractTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("답변 제출 응답은 isCompleted 키를 유지한다")
  void interviewAnswerResponseKeepsIsCompletedKey() throws Exception {
    String json = objectMapper.writeValueAsString(new InterviewAnswerResponse(3, false));

    assertThat(json).contains("\"isCompleted\"").contains("\"nextQuestionNum\"");
  }

  @Test
  @DisplayName("아이디어 응답은 isAiGenerated 키를 유지하고 작성자 정보를 노출하지 않는다")
  void ideaResponseKeepsIsAiGeneratedKeyAndHidesAuthor() throws Exception {
    User author =
        User.builder().email("haeeon03@naver.com").passwordHash("hash").name("이지혜").build();
    IdeaCard card = IdeaCard.ofMember(null, author, "추천 이유를 함께 보여주면 좋겠어요.");

    String json = objectMapper.writeValueAsString(IdeaResponseDto.from(card));

    assertThat(json).contains("\"isAiGenerated\"");
    assertThat(json).contains("\"authorName\":\"익명\"");
    assertThat(json).doesNotContain("이지혜").doesNotContain("haeeon03@naver.com").doesNotContain("userId");
  }
}
