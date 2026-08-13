package com.syncbridge.app.domain.ideaboard.entity;

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

/**
 * DDL: idea_card
 *
 * <p><b>소프트 익명성:</b> 작성자 user_id 는 DB 에 그대로 보존하고(개인 성과 반영 대비), 외부로 나가는 DTO 에서만 "익명" 으로 마스킹한다. 마스킹
 * 해제는 {@code isRevealed} 플래그로 제어한다.
 */
@Entity
@Table(name = "idea_card")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdeaCard {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idea_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "meeting_id")
  private Meeting meeting;

  /** AI 생성 카드인 경우 null. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "content", columnDefinition = "TEXT", nullable = false)
  private String content;

  @Column(name = "is_ai_generated", nullable = false)
  private boolean aiGenerated;

  @Column(name = "is_revealed", nullable = false)
  private boolean revealed;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Builder
  private IdeaCard(Meeting meeting, User user, String content, boolean aiGenerated) {
    this.meeting = meeting;
    this.user = user;
    this.content = content;
    this.aiGenerated = aiGenerated;
    this.revealed = false;
  }

  public static IdeaCard ofMember(Meeting meeting, User user, String content) {
    return IdeaCard.builder().meeting(meeting).user(user).content(content).aiGenerated(false).build();
  }

  public static IdeaCard ofAi(Meeting meeting, String content) {
    return IdeaCard.builder().meeting(meeting).user(null).content(content).aiGenerated(true).build();
  }

  /** 회의 종료 후 작성자 공개 전환. */
  public void reveal() {
    this.revealed = true;
  }
}
