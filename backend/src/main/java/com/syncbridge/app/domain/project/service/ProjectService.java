package com.syncbridge.app.domain.project.service;

import com.syncbridge.app.domain.auth.entity.User;
import com.syncbridge.app.domain.auth.repository.UserRepository;
import com.syncbridge.app.domain.meeting.entity.Meeting;
import com.syncbridge.app.domain.meeting.entity.MeetingStatus;
import com.syncbridge.app.domain.meeting.repository.MeetingRepository;
import com.syncbridge.app.domain.project.dto.JoinProjectRequest;
import com.syncbridge.app.domain.project.dto.JoinProjectResponse;
import com.syncbridge.app.domain.project.dto.ProjectCreateRequest;
import com.syncbridge.app.domain.project.dto.ProjectCreateResponse;
import com.syncbridge.app.domain.project.dto.ProjectMemberCount;
import com.syncbridge.app.domain.project.dto.ProjectStatus;
import com.syncbridge.app.domain.project.dto.ProjectSummaryResponse;
import com.syncbridge.app.domain.project.entity.Project;
import com.syncbridge.app.domain.project.entity.ProjectMember;
import com.syncbridge.app.domain.project.repository.ProjectMemberRepository;
import com.syncbridge.app.domain.project.repository.ProjectRepository;
import com.syncbridge.app.global.error.CustomException;
import com.syncbridge.app.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final MeetingRepository meetingRepository;
  private final UserRepository userRepository;
  private final JoinCodeGenerator joinCodeGenerator;

  @Transactional
  public ProjectCreateResponse createProject(Long userId, ProjectCreateRequest request) {
    User creator = findUser(userId);

    Project project =
        projectRepository.save(
            Project.builder()
                .name(request.name())
                .description(request.description())
                .colorCode(request.color())
                .joinCode(joinCodeGenerator.generate())
                .createdBy(creator)
                .build());

    List<ProjectMember> members = new ArrayList<>();
    members.add(ProjectMember.builder().project(project).user(creator).build());

    // 이미 가입된 사용자만 즉시 멤버로 등록한다. 미가입 이메일은 참여 코드로 합류하도록 안내(메일 발송은 범위 밖).
    if (request.inviteEmails() != null && !request.inviteEmails().isEmpty()) {
      List<User> invitees = userRepository.findAllByEmailIn(request.inviteEmails());
      invitees.stream()
          .filter(invitee -> !invitee.getId().equals(creator.getId()))
          .forEach(
              invitee ->
                  members.add(ProjectMember.builder().project(project).user(invitee).build()));

      if (invitees.size() < request.inviteEmails().size()) {
        log.info(
            "미가입 초대 대상이 있어 참여 코드 안내가 필요합니다. projectId={}, joinCode={}",
            project.getId(),
            project.getJoinCode());
      }
    }
    projectMemberRepository.saveAll(members);

    return ProjectCreateResponse.from(project);
  }

  public List<ProjectSummaryResponse> getMyProjects(Long userId, ProjectStatus statusFilter) {
    List<Project> projects = projectRepository.findAllByMemberUserId(userId);
    if (projects.isEmpty()) {
      return List.of();
    }

    List<Long> projectIds = projects.stream().map(Project::getId).toList();
    Map<Long, Long> memberCounts =
        projectMemberRepository.countByProjectIds(projectIds).stream()
            .collect(
                Collectors.toMap(ProjectMemberCount::projectId, ProjectMemberCount::memberCount));
    Map<Long, List<Meeting>> meetingsByProject =
        meetingRepository.findAllByProjectIdIn(projectIds).stream()
            .collect(Collectors.groupingBy(meeting -> meeting.getProject().getId()));

    ProjectStatus filter = statusFilter == null ? ProjectStatus.ALL : statusFilter;

    return projects.stream()
        .map(
            project ->
                toSummary(
                    project,
                    memberCounts.getOrDefault(project.getId(), 0L),
                    meetingsByProject.getOrDefault(project.getId(), List.of())))
        .filter(summary -> filter == ProjectStatus.ALL || summary.status() == filter)
        .sorted(Comparator.comparing(ProjectSummaryResponse::updatedAt).reversed())
        .toList();
  }

  @Transactional
  public JoinProjectResponse joinByCode(Long userId, JoinProjectRequest request) {
    Project project =
        projectRepository
            .findByJoinCode(request.joinCode().trim().toUpperCase())
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_JOIN_CODE));

    if (projectMemberRepository.existsByProjectIdAndUserId(project.getId(), userId)) {
      throw new CustomException(ErrorCode.ALREADY_JOINED_PROJECT);
    }

    projectMemberRepository.save(
        ProjectMember.builder().project(project).user(findUser(userId)).build());

    return JoinProjectResponse.from(project);
  }

  /** 프로젝트 멤버 권한 검증 후 프로젝트를 반환한다. */
  public Project getMemberProject(Long projectId, Long userId) {
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
    if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
      throw new CustomException(ErrorCode.NOT_PROJECT_MEMBER);
    }
    return project;
  }

  private ProjectSummaryResponse toSummary(Project project, long memberCount, List<Meeting> meetings) {
    boolean completed =
        !meetings.isEmpty()
            && meetings.stream().allMatch(m -> m.getStatus() == MeetingStatus.COMPLETED);

    LocalDateTime updatedAt =
        meetings.stream()
            .map(Meeting::getMeetingAt)
            .max(Comparator.naturalOrder())
            .orElse(project.getCreatedAt());

    return new ProjectSummaryResponse(
        project.getId(),
        project.getName(),
        project.getDescription(),
        project.getColorCode(),
        memberCount,
        project.getCreatedBy() == null ? null : project.getCreatedBy().getName(),
        completed ? ProjectStatus.COMPLETED : ProjectStatus.IN_PROGRESS,
        updatedAt);
  }

  private User findUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
  }

  /** 다른 도메인에서 프로젝트 멤버 여부만 확인할 때 사용. */
  public void validateMember(Long projectId, Long userId) {
    if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
      throw new CustomException(ErrorCode.NOT_PROJECT_MEMBER);
    }
  }
}
