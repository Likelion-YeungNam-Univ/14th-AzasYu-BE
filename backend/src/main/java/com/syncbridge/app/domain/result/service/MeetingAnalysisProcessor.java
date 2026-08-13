package com.syncbridge.app.domain.result.service;

import com.syncbridge.app.domain.ideaboard.service.IdeaService;
import com.syncbridge.app.domain.meeting.entity.Meeting;
import com.syncbridge.app.domain.meeting.entity.MeetingAgenda;
import com.syncbridge.app.domain.meeting.entity.MeetingParticipant;
import com.syncbridge.app.domain.meeting.entity.MeetingStatus;
import com.syncbridge.app.domain.meeting.repository.MeetingParticipantRepository;
import com.syncbridge.app.domain.meeting.repository.MeetingRepository;
import com.syncbridge.app.domain.result.dto.AiAnalysisResponse;
import com.syncbridge.app.domain.result.entity.MeetingResult;
import com.syncbridge.app.domain.result.repository.MeetingResultRepository;
import com.syncbridge.app.global.config.AsyncConfig;
import com.syncbridge.app.global.error.CustomException;
import com.syncbridge.app.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 업로드된 회의록의 AI 분석을 백그라운드에서 수행한다.
 *
 * <p>업로드 API 는 202 Accepted 로 즉시 응답하고, 실제 FastAPI 호출/결과 저장은 이 컴포넌트가 별도 스레드에서 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingAnalysisProcessor {

  private final MeetingRepository meetingRepository;
  private final MeetingParticipantRepository meetingParticipantRepository;
  private final MeetingResultRepository meetingResultRepository;
  private final AiAnalysisClient aiAnalysisClient;
  private final AnalysisTaskRegistry taskRegistry;
  private final IdeaService ideaService;

  @Async(AsyncConfig.ANALYSIS_EXECUTOR)
  @Transactional
  public void process(
      String taskId, Long meetingId, FileStorageService.StoredFile file, String rawText) {
    try {
      Meeting meeting =
          meetingRepository
              .findWithProjectById(meetingId)
              .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));

      List<String> agendas = meeting.getAgendas().stream().map(MeetingAgenda::getContent).toList();
      List<String> participantNames =
          meetingParticipantRepository.findAllByMeetingId(meetingId).stream()
              .map(MeetingParticipant::getUser)
              .map(user -> user.getName())
              .toList();

      AiAnalysisResponse analysis =
          aiAnalysisClient.summarize(
              file, rawText, meeting.getTitle(), meeting.getPurpose(), agendas, participantNames);

      meetingResultRepository
          .findByMeetingId(meetingId)
          .ifPresentOrElse(
              existing ->
                  existing.update(
                      analysis.purpose(),
                      analysis.mainDiscussions(),
                      analysis.decisions(),
                      analysis.actionItems(),
                      analysis.misunderstandings()),
              () ->
                  meetingResultRepository.save(
                      MeetingResult.builder()
                          .meeting(meeting)
                          .purposeSummary(analysis.purpose())
                          .mainDiscussions(analysis.mainDiscussions())
                          .decisions(analysis.decisions())
                          .actionItems(analysis.actionItems())
                          .misunderstandings(analysis.misunderstandings())
                          .build()));

      // 선순환 구조: AI 가 감지한 오해 리스크를 아이디어 보드에 카드로 게시한다.
      ideaService.createAiRiskCards(meeting, analysis.misunderstandings());

      meeting.changeStatus(MeetingStatus.COMPLETED);
      taskRegistry.markCompleted(taskId);
      log.info("회의록 AI 분석 완료. taskId={}, meetingId={}", taskId, meetingId);
    } catch (RuntimeException e) {
      taskRegistry.markFailed(taskId);
      log.error("회의록 AI 분석 실패. taskId={}, meetingId={}", taskId, meetingId, e);
      throw e;
    }
  }
}
