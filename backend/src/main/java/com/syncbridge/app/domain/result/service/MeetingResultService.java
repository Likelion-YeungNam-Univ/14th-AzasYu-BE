package com.syncbridge.app.domain.result.service;

import com.syncbridge.app.domain.meeting.entity.Meeting;
import com.syncbridge.app.domain.meeting.service.MeetingService;
import com.syncbridge.app.domain.result.dto.MeetingResultResponse;
import com.syncbridge.app.domain.result.dto.MeetingUploadResponse;
import com.syncbridge.app.domain.result.entity.MeetingResult;
import com.syncbridge.app.domain.result.repository.MeetingResultRepository;
import com.syncbridge.app.global.error.CustomException;
import com.syncbridge.app.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingResultService {

  private final MeetingService meetingService;
  private final MeetingResultRepository meetingResultRepository;
  private final FileStorageService fileStorageService;
  private final MeetingAnalysisProcessor analysisProcessor;
  private final AnalysisTaskRegistry taskRegistry;

  /** 회의록 업로드 → 검증/저장 후 비동기 AI 분석을 시작한다 (202 Accepted). */
  public MeetingUploadResponse requestAnalysis(
      Long meetingId, Long userId, MultipartFile file, String rawText) {
    meetingService.getAccessibleMeeting(meetingId, userId);

    boolean hasFile = file != null && !file.isEmpty();
    boolean hasText = rawText != null && !rawText.isBlank();
    if (!hasFile && !hasText) {
      throw new CustomException(ErrorCode.EMPTY_UPLOAD);
    }

    FileStorageService.StoredFile storedFile = hasFile ? fileStorageService.store(file) : null;
    String taskId = taskRegistry.createTask();

    analysisProcessor.process(taskId, meetingId, storedFile, hasText ? rawText : null);

    return MeetingUploadResponse.processing(taskId);
  }

  public MeetingResultResponse getResult(Long meetingId, Long userId) {
    Meeting meeting = meetingService.getAccessibleMeeting(meetingId, userId);
    MeetingResult result =
        meetingResultRepository
            .findByMeetingId(meetingId)
            .orElseThrow(() -> new CustomException(ErrorCode.MEETING_RESULT_NOT_FOUND));
    return MeetingResultResponse.of(meeting, result);
  }
}
