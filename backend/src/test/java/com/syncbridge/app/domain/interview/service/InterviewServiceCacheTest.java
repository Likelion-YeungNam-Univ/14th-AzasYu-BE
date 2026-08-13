package com.syncbridge.app.domain.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncbridge.app.domain.auth.entity.User;
import com.syncbridge.app.domain.auth.repository.UserRepository;
import com.syncbridge.app.domain.interview.dto.InterviewAnswerRequest;
import com.syncbridge.app.domain.interview.repository.PreInterviewAnswerRepository;
import com.syncbridge.app.domain.meeting.entity.Meeting;
import com.syncbridge.app.domain.meeting.repository.MeetingRepository;
import com.syncbridge.app.domain.meeting.service.MeetingService;
import com.syncbridge.app.domain.result.repository.MeetingResultRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;

/** 인터뷰 질문 캐싱 동작 검증 (중복 LLM 과금 방지). */
@ExtendWith(MockitoExtension.class)
class InterviewServiceCacheTest {

  private static final Long MEETING_ID = 100L;
  private static final Long USER_ID = 1L;
  private static final String CACHED_QUESTION = "말씀하신 '핵심 기능'은 구체적으로 어디까지 포함된다고 생각하시나요?";

  @Mock private WebClient aiServiceWebClient;
  @Mock private MeetingService meetingService;
  @Mock private MeetingRepository meetingRepository;
  @Mock private MeetingResultRepository meetingResultRepository;
  @Mock private PreInterviewAnswerRepository preInterviewAnswerRepository;
  @Mock private UserRepository userRepository;
  @Mock private InterviewQuestionCache questionCache;
  @Spy private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks private InterviewService interviewService;

  @Test
  @DisplayName("캐시된 질문이 있으면 AI 서비스를 호출하지 않고 그대로 재생한다")
  void cacheHitSkipsAiCall() {
    given(meetingService.getAccessibleMeeting(MEETING_ID, USER_ID)).willReturn(meeting());
    given(questionCache.find(MEETING_ID, USER_ID, 2)).willReturn(Optional.of(CACHED_QUESTION));

    List<ServerSentEvent<String>> events =
        interviewService.streamQuestion(MEETING_ID, USER_ID, 2).collectList().block();

    assertThat(events).isNotNull();
    // LLM 재호출이 전혀 없어야 한다.
    verifyNoInteractions(aiServiceWebClient);
    verify(questionCache, never()).save(anyLong(), anyLong(), anyInt(), any());

    // 마지막 이벤트는 DONE, 나머지 청크를 이어 붙이면 원본 질문이 복원된다.
    String last = events.get(events.size() - 1).data();
    assertThat(last).contains("\"status\":\"DONE\"").contains("\"questionNum\":2");

    String reassembled =
        events.subList(0, events.size() - 1).stream()
            .map(event -> readChunk(event.data()))
            .reduce("", String::concat);
    assertThat(reassembled).isEqualTo(CACHED_QUESTION);
  }

  @Test
  @DisplayName("답변이 제출되면 그보다 뒤 번호의 질문 캐시를 무효화한다")
  void answerInvalidatesLaterQuestionCache() {
    Meeting meeting = meeting();
    given(meetingService.getAccessibleMeeting(MEETING_ID, USER_ID)).willReturn(meeting);
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));
    given(preInterviewAnswerRepository.findByMeetingIdAndUserIdAndQuestionNum(MEETING_ID, USER_ID, 2))
        .willReturn(Optional.empty());

    interviewService.submitAnswer(
        MEETING_ID,
        USER_ID,
        new InterviewAnswerRequest(2, "핵심 기능은 어디까지인가요?", "추천 기능만 포함된다고 생각해요."));

    verify(questionCache).invalidateAfter(MEETING_ID, USER_ID, 2);
  }

  @Test
  @DisplayName("질문 번호가 1~6 범위를 벗어나면 캐시 조회 전에 거절한다")
  void rejectsOutOfRangeQuestionNumber() {
    assertThat(
            org.assertj.core.api.Assertions.catchThrowable(
                () -> interviewService.streamQuestion(MEETING_ID, USER_ID, 7)))
        .hasMessageContaining("1~6");

    verifyNoInteractions(questionCache, aiServiceWebClient);
  }

  private String readChunk(String data) {
    try {
      return objectMapper.readTree(data).get("chunk").asText();
    } catch (Exception e) {
      throw new IllegalStateException("청크 파싱 실패: " + data, e);
    }
  }

  private Meeting meeting() {
    return Meeting.builder()
        .title("6차 기획 회의")
        .purpose("서비스 방향 설정")
        .meetingAt(LocalDateTime.of(2026, 8, 12, 14, 0))
        .durationMinutes(90)
        .build();
  }

  private User user() {
    return User.builder().email("haeeon03@naver.com").passwordHash("hash").name("이지혜").build();
  }
}
