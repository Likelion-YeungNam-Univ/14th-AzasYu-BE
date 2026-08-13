package com.syncbridge.app.domain.meeting.entity;

import com.syncbridge.app.domain.auth.entity.User;
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
 * 회의 참석자.
 *
 * <p>SPEC 3.1 DDL 에는 없지만, `POST /projects/{id}/meetings` 의 `participantUserIds` 저장과 회의 대시보드의
 * `totalParticipantCount` 산출을 위해 추가한 확장 테이블이다. (프로젝트 전체 멤버 ≠ 특정 회의 참석자)
 */
@Entity
@Table(
    name = "meeting_participant",
    uniqueConstraints =
        @UniqueConstraint(
            name = "unique_meeting_user",
            columnNames = {"meeting_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingParticipant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "participant_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "meeting_id")
  private Meeting meeting;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Builder
  private MeetingParticipant(Meeting meeting, User user) {
    this.meeting = meeting;
    this.user = user;
  }
}
