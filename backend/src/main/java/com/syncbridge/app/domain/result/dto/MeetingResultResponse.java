package com.syncbridge.app.domain.result.dto;

import com.syncbridge.app.domain.meeting.entity.Meeting;
import com.syncbridge.app.domain.result.entity.ActionItem;
import com.syncbridge.app.domain.result.entity.MeetingResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Schema(description = "회의 결과 리포트")
public record MeetingResultResponse(
    @Schema(description = "회의 ID", example = "100") Long meetingId,
    @Schema(description = "리포트 제목", example = "6차 기획 회의 결과") String title,
    @Schema(description = "회의 일시(표시용)", example = "2026.08.12 오후 2:00-4:00") String meetingAt,
    @Schema(description = "회의 목적 요약") String purpose,
    @Schema(description = "주요 논의 내용") List<String> mainDiscussions,
    @Schema(description = "결정 사항") List<String> decisions,
    @Schema(description = "실행 항목") List<ActionItem> actionItems,
    @Schema(description = "다르게 이해될 수 있는 부분(오해 리스크)") List<String> misunderstandings) {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN);

  public static MeetingResultResponse of(Meeting meeting, MeetingResult result) {
    return new MeetingResultResponse(
        meeting.getId(),
        meeting.getTitle() + " 결과",
        formatMeetingPeriod(meeting.getMeetingAt(), meeting.getDurationMinutes()),
        result.getPurposeSummary(),
        result.getMainDiscussions(),
        result.getDecisions(),
        result.getActionItems(),
        result.getMisunderstandings());
  }

  /** "2026.08.12 오후 2:00-4:00" 형식으로 변환한다. */
  private static String formatMeetingPeriod(LocalDateTime meetingAt, Integer durationMinutes) {
    LocalDateTime endAt = meetingAt.plusMinutes(durationMinutes == null ? 60 : durationMinutes);
    return "%s %s %s-%s"
        .formatted(
            DATE_FORMAT.format(meetingAt),
            meetingAt.getHour() < 12 ? "오전" : "오후",
            formatTime(meetingAt),
            formatTime(endAt));
  }

  private static String formatTime(LocalDateTime time) {
    int hour = time.getHour() % 12;
    return "%d:%02d".formatted(hour == 0 ? 12 : hour, time.getMinute());
  }
}
