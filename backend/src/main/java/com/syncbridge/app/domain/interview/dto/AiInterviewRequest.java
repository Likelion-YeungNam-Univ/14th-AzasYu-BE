package com.syncbridge.app.domain.interview.dto;

import java.util.List;

/**
 * FastAPI(`POST /ai/interview/stream`) 로 전달하는 인터뷰 컨텍스트.
 *
 * <p>이전 회의 결과의 오해 리스크 + 현재 회의의 목적/안건 + 해당 사용자의 이전 답변을 함께 넘겨 "개별 오해 가능성 추적" 질문을 생성하도록 한다.
 */
public record AiInterviewRequest(
    Long meetingId,
    int questionNum,
    int totalQuestions,
    String participantName,
    String meetingTitle,
    String purpose,
    List<String> agendas,
    List<PreviousAnswer> previousAnswers,
    List<String> previousMeetingMisunderstandings) {

  public record PreviousAnswer(int questionNum, String questionText, String answerText) {}
}
