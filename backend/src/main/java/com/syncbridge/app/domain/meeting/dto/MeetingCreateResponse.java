package com.syncbridge.app.domain.meeting.dto;

import com.syncbridge.app.domain.meeting.entity.Meeting;
import com.syncbridge.app.domain.meeting.entity.MeetingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회의 생성 응답")
public record MeetingCreateResponse(
    @Schema(description = "회의 ID", example = "100") Long meetingId,
    @Schema(description = "회의 제목", example = "6차 기획 회의") String title,
    @Schema(description = "회의 상태", example = "BEFORE_INTERVIEW") MeetingStatus status) {

  public static MeetingCreateResponse from(Meeting meeting) {
    return new MeetingCreateResponse(meeting.getId(), meeting.getTitle(), meeting.getStatus());
  }
}
