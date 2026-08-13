package com.syncbridge.app.domain.interview.entity;

import com.syncbridge.app.domain.auth.entity.User;
import com.syncbridge.app.domain.meeting.entity.Meeting;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 생성된 사전 인터뷰 질문 캐시.
 *
 * <p>SPEC 3.1 DDL 에는 없는 확장 테이블이다. 스트림 엔드포인트는 호출될 때마다 LLM 을 새로 호출하므로,
 * 사용자가 새로고침하거나 재접속하면 같은 질문에 대해 중복 과금이 발생한다. 생성된 질문을
 * (meeting, user, questionNum) 단위로 저장해 재요청 시 LLM 호출 없이 재생한다.
 *
 * <p>질문 N 은 답변 1..N-1 을 컨텍스트로 생성되므로, 답변이 수정되면 그보다 뒤 번호의 캐시는
 * 무효화해야 한다. ({@code InterviewService#submitAnswer})
 */
@Entity
@Table(
    name = "interview_question",
    uniqueConstraints =
        @UniqueConstraint(
            name = "unique_meeting_user_question",
            columnNames = {"meeting_id", "user_id", "question_num"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewQuestion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "question_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "meeting_id")
  private Meeting meeting;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "question_num", nullable = false)
  private int questionNum;

  @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
  private String questionText;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Builder
  private InterviewQuestion(Meeting meeting, User user, int questionNum, String questionText) {
    this.meeting = meeting;
    this.user = user;
    this.questionNum = questionNum;
    this.questionText = questionText;
  }
}
