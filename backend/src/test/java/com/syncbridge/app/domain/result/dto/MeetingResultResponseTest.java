package com.syncbridge.app.domain.result.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.syncbridge.app.domain.meeting.entity.Meeting;
import com.syncbridge.app.domain.result.entity.ActionItem;
import com.syncbridge.app.domain.result.entity.MeetingResult;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MeetingResultResponseTest {

  @Test
  @DisplayName("회의 일시를 '2026.08.12 오후 2:00-4:00' 형식으로 변환한다")
  void formatsMeetingPeriodInKorean() {
    Meeting meeting =
        Meeting.builder()
            .title("6차 기획 회의")
            .purpose("서비스 방향 설정")
            .meetingAt(LocalDateTime.of(2026, 8, 12, 14, 0))
            .durationMinutes(120)
            .build();
    MeetingResult result =
        MeetingResult.builder()
            .meeting(meeting)
            .purposeSummary("새로운 서비스의 방향을 설정했습니다.")
            .mainDiscussions(List.of("사용자 불편을 먼저 파악하기로 했습니다."))
            .decisions(List.of("초기 버전은 핵심 기능에 집중합니다."))
            .actionItems(List.of(new ActionItem("이지혜", "사용자 문제 및 핵심 타깃 정의")))
            .misunderstandings(List.of("1. 핵심 기능의 범위 기준이 명확하지 않습니다."))
            .build();

    MeetingResultResponse response = MeetingResultResponse.of(meeting, result);

    assertThat(response.meetingAt()).isEqualTo("2026.08.12 오후 2:00-4:00");
    assertThat(response.title()).isEqualTo("6차 기획 회의 결과");
    assertThat(response.actionItems()).containsExactly(new ActionItem("이지혜", "사용자 문제 및 핵심 타깃 정의"));
  }

  @Test
  @DisplayName("오전 회의와 정오 시작 회의를 올바르게 표기한다")
  void formatsMorningAndNoonMeetings() {
    Meeting morning =
        Meeting.builder()
            .title("스탠드업")
            .purpose("데일리 공유")
            .meetingAt(LocalDateTime.of(2026, 8, 12, 9, 30))
            .durationMinutes(30)
            .build();
    Meeting noon =
        Meeting.builder()
            .title("점심 리뷰")
            .purpose("리뷰")
            .meetingAt(LocalDateTime.of(2026, 8, 12, 12, 0))
            .durationMinutes(60)
            .build();
    MeetingResult emptyResult = MeetingResult.builder().purposeSummary("").build();

    assertThat(MeetingResultResponse.of(morning, emptyResult).meetingAt())
        .isEqualTo("2026.08.12 오전 9:30-10:00");
    assertThat(MeetingResultResponse.of(noon, emptyResult).meetingAt())
        .isEqualTo("2026.08.12 오후 12:00-1:00");
  }
}
