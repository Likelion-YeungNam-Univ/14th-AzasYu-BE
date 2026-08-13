package com.syncbridge.app.domain.meeting.service;

import com.syncbridge.app.domain.auth.entity.User;
import com.syncbridge.app.domain.auth.repository.UserRepository;
import com.syncbridge.app.domain.interview.repository.PreInterviewAnswerRepository;
import com.syncbridge.app.domain.meeting.dto.MeetingCreateRequest;
import com.syncbridge.app.domain.meeting.dto.MeetingCreateResponse;
import com.syncbridge.app.domain.meeting.dto.MeetingDetailResponse;
import com.syncbridge.app.domain.meeting.entity.Meeting;
import com.syncbridge.app.domain.meeting.entity.MeetingAgenda;
import com.syncbridge.app.domain.meeting.repository.MeetingParticipantRepository;
import com.syncbridge.app.domain.meeting.repository.MeetingRepository;
import com.syncbridge.app.domain.project.entity.Project;
import com.syncbridge.app.domain.project.service.ProjectService;
import com.syncbridge.app.global.error.CustomException;
import com.syncbridge.app.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {

  /** 사전 인터뷰 총 질문 수 (SPEC 2.4: questionNum 1~6). */
  public static final int TOTAL_INTERVIEW_QUESTIONS = 6;

  private final MeetingRepository meetingRepository;
  private final MeetingParticipantRepository meetingParticipantRepository;
  private final PreInterviewAnswerRepository preInterviewAnswerRepository;
  private final UserRepository userRepository;
  private final ProjectService projectService;

  @Transactional
  public MeetingCreateResponse createMeeting(
      Long projectId, Long userId, MeetingCreateRequest request) {
    Project project = projectService.getMemberProject(projectId, userId);

    Meeting meeting =
        Meeting.builder()
            .project(project)
            .title(request.title())
            .purpose(request.purpose())
            .meetingAt(request.meetingAt())
            .durationMinutes(request.durationMinutes())
            .build();

    if (request.agendas() != null) {
      int orderIndex = 0;
      for (String agenda : request.agendas()) {
        if (agenda != null && !agenda.isBlank()) {
          meeting.addAgenda(agenda, orderIndex++);
        }
      }
    }

    List<Long> participantIds =
        (request.participantUserIds() == null || request.participantUserIds().isEmpty())
            ? List.of(userId)
            : request.participantUserIds();
    for (User participant : userRepository.findAllById(participantIds)) {
      projectService.validateMember(projectId, participant.getId());
      meeting.addParticipant(participant);
    }

    return MeetingCreateResponse.from(meetingRepository.save(meeting));
  }

  public MeetingDetailResponse getMeetingDetail(Long meetingId, Long userId) {
    Meeting meeting = getAccessibleMeeting(meetingId, userId);
    Project project = meeting.getProject();

    long completedInterviewCount =
        preInterviewAnswerRepository.countCompletedUsers(meetingId, TOTAL_INTERVIEW_QUESTIONS);
    long totalParticipantCount = meetingParticipantRepository.countByMeetingId(meetingId);

    return new MeetingDetailResponse(
        meeting.getId(),
        project.getName(),
        project.getJoinCode(),
        meeting.getTitle(),
        meeting.getPurpose(),
        meeting.getAgendas().stream().map(MeetingAgenda::getContent).toList(),
        meeting.getMeetingAt(),
        meeting.getDurationMinutes(),
        completedInterviewCount,
        totalParticipantCount,
        meeting.getStatus());
  }

  /** 회의 조회 + 프로젝트 멤버 권한 검증. 다른 도메인(interview/ideaboard/result)에서 공통으로 사용한다. */
  public Meeting getAccessibleMeeting(Long meetingId, Long userId) {
    Meeting meeting =
        meetingRepository
            .findWithProjectById(meetingId)
            .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));
    projectService.validateMember(meeting.getProject().getId(), userId);
    return meeting;
  }
}
