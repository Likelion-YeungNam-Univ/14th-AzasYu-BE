package com.syncbridge.app.domain.result.entity;

import com.syncbridge.app.domain.meeting.entity.Meeting;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** DDL: meeting_result (JSONB 컬럼은 Hibernate 6 의 {@code @JdbcTypeCode(SqlTypes.JSON)} 으로 매핑) */
@Entity
@Table(name = "meeting_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "result_id")
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "meeting_id", unique = true)
  private Meeting meeting;

  @Column(name = "purpose_summary", columnDefinition = "TEXT")
  private String purposeSummary;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "main_discussions", columnDefinition = "jsonb")
  private List<String> mainDiscussions = new ArrayList<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "decisions", columnDefinition = "jsonb")
  private List<String> decisions = new ArrayList<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "action_items", columnDefinition = "jsonb")
  private List<ActionItem> actionItems = new ArrayList<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "misunderstandings", columnDefinition = "jsonb")
  private List<String> misunderstandings = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Builder
  private MeetingResult(
      Meeting meeting,
      String purposeSummary,
      List<String> mainDiscussions,
      List<String> decisions,
      List<ActionItem> actionItems,
      List<String> misunderstandings) {
    this.meeting = meeting;
    update(purposeSummary, mainDiscussions, decisions, actionItems, misunderstandings);
  }

  /** 재분석 시 기존 결과를 덮어쓴다 (meeting_id 는 UNIQUE). */
  public void update(
      String purposeSummary,
      List<String> mainDiscussions,
      List<String> decisions,
      List<ActionItem> actionItems,
      List<String> misunderstandings) {
    this.purposeSummary = purposeSummary;
    this.mainDiscussions = nullSafe(mainDiscussions);
    this.decisions = nullSafe(decisions);
    this.actionItems = nullSafe(actionItems);
    this.misunderstandings = nullSafe(misunderstandings);
  }

  private static <T> List<T> nullSafe(List<T> values) {
    return values == null ? new ArrayList<>() : new ArrayList<>(values);
  }
}
