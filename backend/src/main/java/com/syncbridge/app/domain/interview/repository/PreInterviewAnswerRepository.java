package com.syncbridge.app.domain.interview.repository;

import com.syncbridge.app.domain.interview.entity.PreInterviewAnswer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PreInterviewAnswerRepository extends JpaRepository<PreInterviewAnswer, Long> {

  List<PreInterviewAnswer> findAllByMeetingIdAndUserIdOrderByQuestionNumAsc(
      Long meetingId, Long userId);

  List<PreInterviewAnswer> findAllByMeetingIdOrderByUserIdAscQuestionNumAsc(Long meetingId);

  Optional<PreInterviewAnswer> findByMeetingIdAndUserIdAndQuestionNum(
      Long meetingId, Long userId, int questionNum);

  /** 전체 질문(:totalQuestions 개)에 모두 답변한 사용자 수 = 인터뷰 완료자 수. */
  @Query(
      """
      select count(distinct a.user.id) from PreInterviewAnswer a
      where a.meeting.id = :meetingId
        and a.user.id in (
          select b.user.id from PreInterviewAnswer b
          where b.meeting.id = :meetingId
          group by b.user.id
          having count(distinct b.questionNum) >= :totalQuestions
        )
      """)
  long countCompletedUsers(
      @Param("meetingId") Long meetingId, @Param("totalQuestions") long totalQuestions);
}
