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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/** DDL: pre_interview_answer */
@Entity
@Table(name = "pre_interview_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PreInterviewAnswer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "answer_id")
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

  @Column(name = "answer_text", columnDefinition = "TEXT", nullable = false)
  private String answerText;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Builder
  private PreInterviewAnswer(
      Meeting meeting, User user, int questionNum, String questionText, String answerText) {
    this.meeting = meeting;
    this.user = user;
    this.questionNum = questionNum;
    this.questionText = questionText;
    this.answerText = answerText;
  }

  public void updateAnswer(String questionText, String answerText) {
    this.questionText = questionText;
    this.answerText = answerText;
  }
}
