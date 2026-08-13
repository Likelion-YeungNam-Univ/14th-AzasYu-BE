package com.syncbridge.app.domain.project.entity;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/** DDL: project */
@Entity
@Table(name = "project")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

  public static final String DEFAULT_COLOR = "#4A90E2";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "project_id")
  private Long id;

  @Column(name = "name", length = 100, nullable = false)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "color_code", length = 10, nullable = false)
  private String colorCode;

  @Column(name = "join_code", length = 20, nullable = false, unique = true)
  private String joinCode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Builder
  private Project(
      String name, String description, String colorCode, String joinCode, User createdBy) {
    this.name = name;
    this.description = description;
    this.colorCode = (colorCode == null || colorCode.isBlank()) ? DEFAULT_COLOR : colorCode;
    this.joinCode = joinCode;
    this.createdBy = createdBy;
  }
}
