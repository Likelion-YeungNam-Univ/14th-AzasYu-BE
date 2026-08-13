package com.syncbridge.app.domain.interview.service;

import com.syncbridge.app.domain.auth.repository.UserRepository;
import com.syncbridge.app.domain.interview.entity.InterviewQuestion;
import com.syncbridge.app.domain.interview.repository.InterviewQuestionRepository;
import com.syncbridge.app.domain.meeting.repository.MeetingRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 생성된 인터뷰 질문의 조회/저장을 담당한다.
 *
 * <p>저장은 SSE 스트림이 끝나는 시점(Reactor 콜백 스레드)에 호출되므로, 요청 스레드의 트랜잭션과 무관하게
 * 항상 새 트랜잭션에서 수행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewQuestionCache {

  private final InterviewQuestionRepository interviewQuestionRepository;
  private final MeetingRepository meetingRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public Optional<String> find(Long meetingId, Long userId, int questionNum) {
    return interviewQuestionRepository
        .findByMeetingIdAndUserIdAndQuestionNum(meetingId, userId, questionNum)
        .map(InterviewQuestion::getQuestionText);
  }

  /** 스트림 완료 후 생성된 질문을 저장한다. 이미 있으면 아무것도 하지 않는다. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void save(Long meetingId, Long userId, int questionNum, String questionText) {
    if (questionText == null || questionText.isBlank()) {
      return;
    }
    if (interviewQuestionRepository.existsByMeetingIdAndUserIdAndQuestionNum(
        meetingId, userId, questionNum)) {
      return;
    }

    try {
      interviewQuestionRepository.save(
          InterviewQuestion.builder()
              .meeting(meetingRepository.getReferenceById(meetingId))
              .user(userRepository.getReferenceById(userId))
              .questionNum(questionNum)
              .questionText(questionText.trim())
              .build());
      log.debug("인터뷰 질문 캐시 저장. meetingId={}, userId={}, q={}", meetingId, userId, questionNum);
    } catch (DataIntegrityViolationException e) {
      // 같은 사용자가 동시에 두 번 스트리밍한 경우 — UNIQUE 제약으로 한쪽만 저장되면 충분하다.
      log.debug("인터뷰 질문 캐시 중복 저장 무시. meetingId={}, q={}", meetingId, questionNum);
    }
  }

  /** 답변 변경으로 컨텍스트가 달라진 이후 번호의 캐시를 무효화한다. */
  @Transactional
  public void invalidateAfter(Long meetingId, Long userId, int questionNum) {
    interviewQuestionRepository.deleteByMeetingIdAndUserIdAndQuestionNumGreaterThan(
        meetingId, userId, questionNum);
  }
}
