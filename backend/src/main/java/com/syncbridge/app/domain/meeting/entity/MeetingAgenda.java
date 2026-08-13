package com.syncbridge.app.domain.meeting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** DDL: meeting_agenda */
@Entity
@Table(name = "meeting_agenda")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingAgenda {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "agenda_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "meeting_id")
  private Meeting meeting;

  @Column(name = "content", length = 255, nullable = false)
  private String content;

  @Column(name = "order_index", nullable = false)
  private int orderIndex;

  @Builder
  private MeetingAgenda(Meeting meeting, String content, int orderIndex) {
    this.meeting = meeting;
    this.content = content;
    this.orderIndex = orderIndex;
  }
}
