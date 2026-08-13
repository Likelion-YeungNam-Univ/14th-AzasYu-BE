package com.syncbridge.app.domain.interview.repository;

import com.syncbridge.app.domain.interview.entity.InterviewQuestion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

  Optional<InterviewQuestion> findByMeetingIdAndUserIdAndQuestionNum(
      Long meetingId, Long userId, int questionNum);

  boolean existsByMeetingIdAndUserIdAndQuestionNum(Long meetingId, Long userId, int questionNum);

  /** 답변이 바뀌어 컨텍스트가 달라진 이후 번호의 질문 캐시를 무효화한다. */
  void deleteByMeetingIdAndUserIdAndQuestionNumGreaterThan(
      Long meetingId, Long userId, int questionNum);
}
