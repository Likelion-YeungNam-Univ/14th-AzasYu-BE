package com.syncbridge.app.domain.meeting.entity;

import com.syncbridge.app.domain.project.entity.Project;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/** DDL: meeting */
@Entity
@Table(name = "meeting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "meeting_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  private Project project;

  @Column(name = "title", length = 100, nullable = false)
  private String title;

  @Column(name = "purpose", columnDefinition = "TEXT", nullable = false)
  private String purpose;

  @Column(name = "meeting_at", nullable = false)
  private LocalDateTime meetingAt;

  @Column(name = "duration_minutes")
  private Integer durationMinutes;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20)
  private MeetingStatus status;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("orderIndex ASC")
  private List<MeetingAgenda> agendas = new ArrayList<>();

  @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<MeetingParticipant> participants = new ArrayList<>();

  @Builder
  private Meeting(
      Project project,
      String title,
      String purpose,
      LocalDateTime meetingAt,
      Integer durationMinutes) {
    this.project = project;
    this.title = title;
    this.purpose = purpose;
    this.meetingAt = meetingAt;
    this.durationMinutes = durationMinutes == null ? 60 : durationMinutes;
    this.status = MeetingStatus.BEFORE_INTERVIEW;
  }

  public void addAgenda(String content, int orderIndex) {
    agendas.add(MeetingAgenda.builder().meeting(this).content(content).orderIndex(orderIndex).build());
  }

  public void addParticipant(com.syncbridge.app.domain.auth.entity.User user) {
    participants.add(MeetingParticipant.builder().meeting(this).user(user).build());
  }

  public void changeStatus(MeetingStatus status) {
    this.status = status;
  }
}
