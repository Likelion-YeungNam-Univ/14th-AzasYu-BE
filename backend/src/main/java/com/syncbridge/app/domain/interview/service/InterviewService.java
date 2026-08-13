package com.syncbridge.app.domain.interview.service;

import com.syncbridge.app.domain.auth.entity.User;
import com.syncbridge.app.domain.auth.repository.UserRepository;
import com.syncbridge.app.domain.interview.dto.AiInterviewRequest;
import com.syncbridge.app.domain.interview.dto.InterviewAnswerRequest;
import com.syncbridge.app.domain.interview.dto.InterviewAnswerResponse;
import com.syncbridge.app.domain.interview.entity.PreInterviewAnswer;
import com.syncbridge.app.domain.interview.repository.PreInterviewAnswerRepository;
import com.syncbridge.app.domain.meeting.entity.Meeting;
import com.syncbridge.app.domain.meeting.entity.MeetingAgenda;
import com.syncbridge.app.domain.meeting.entity.MeetingStatus;
import com.syncbridge.app.domain.meeting.repository.MeetingRepository;
import com.syncbridge.app.domain.meeting.service.MeetingService;
import com.syncbridge.app.domain.result.repository.MeetingResultRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncbridge.app.global.error.CustomException;
import com.syncbridge.app.global.error.ErrorCode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AI 사전 인터뷰 서비스.
 *
 * <p>질문 생성은 FastAPI(LangChain + GPT-4o)가 담당하며, Spring 은 WebClient 로 그 스트림을 받아 클라이언트에게 그대로 중계(SSE
 * Proxy)한다. 인터뷰 컨텍스트(회의 목적/안건, 사용자의 이전 답변, 직전 회의의 오해 리스크)는 Spring 이 DB 에서 조립해 전달한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {

  private static final String AI_STREAM_PATH = "/ai/interview/stream";
  private static final String DONE_PAYLOAD_FORMAT = "{\"status\":\"DONE\",\"questionNum\":%d}";
  private static final String ERROR_PAYLOAD_FORMAT =
      "{\"status\":\"ERROR\",\"errorCode\":\"%s\",\"message\":\"%s\"}";

  /** 캐시된 질문을 재생할 때의 청크 크기와 간격 (타자기 효과 유지). */
  private static final int REPLAY_CHUNK_SIZE = 6;

  private static final Duration REPLAY_CHUNK_DELAY = Duration.ofMillis(30);

  private final WebClient aiServiceWebClient;
  private final MeetingService meetingService;
  private final MeetingRepository meetingRepository;
  private final MeetingResultRepository meetingResultRepository;
  private final PreInterviewAnswerRepository preInterviewAnswerRepository;
  private final UserRepository userRepository;
  private final InterviewQuestionCache questionCache;
  private final ObjectMapper objectMapper;

  /**
   * FastAPI 의 SSE 스트림을 프론트엔드로 중계한다.
   *
   * <p>DB 조회(블로킹)는 Flux 를 구독하기 전에 호출 스레드에서 끝내고, 이후 네트워크 스트림만 리액티브하게 흘려보낸다.
   */
  public Flux<ServerSentEvent<String>> streamQuestion(Long meetingId, Long userId, int questionNum) {
    if (questionNum < 1 || questionNum > MeetingService.TOTAL_INTERVIEW_QUESTIONS) {
      throw new CustomException(ErrorCode.INVALID_QUESTION_NUM);
    }

    meetingService.getAccessibleMeeting(meetingId, userId);

    // 1) 이미 생성된 질문이 있으면 LLM 을 호출하지 않고 그대로 재생한다 (중복 과금 방지).
    Optional<String> cached = questionCache.find(meetingId, userId, questionNum);
    if (cached.isPresent()) {
      log.debug("인터뷰 질문 캐시 히트. meetingId={}, userId={}, q={}", meetingId, userId, questionNum);
      return replayCachedQuestion(cached.get(), questionNum);
    }

    // 2) 캐시 미스 — FastAPI 스트림을 중계하면서 청크를 모아 두었다가 완료 시 저장한다.
    AiInterviewRequest aiRequest = buildContext(meetingId, userId, questionNum);
    AtomicBoolean doneEmitted = new AtomicBoolean(false);
    StringBuilder generated = new StringBuilder();

    return aiServiceWebClient
        .post()
        .uri(AI_STREAM_PATH)
        .accept(MediaType.TEXT_EVENT_STREAM)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(aiRequest)
        .retrieve()
        .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
        .mapNotNull(
            upstream -> {
              String data = upstream.data();
              if (data == null || data.isBlank()) {
                return null;
              }
              if (data.contains("\"DONE\"")) {
                doneEmitted.set(true);
              } else {
                extractChunk(data).ifPresent(generated::append);
              }
              return ServerSentEvent.<String>builder().data(data).build();
            })
        // 정상 완료된 경우에만 캐시에 저장한다. (onErrorResume 앞에 두어 실패 시 저장되지 않게 함)
        .doOnComplete(() -> questionCache.save(meetingId, userId, questionNum, generated.toString()))
        // 업스트림이 DONE 을 보내지 않고 끝나도 클라이언트가 EventSource 를 닫을 수 있도록 보정한다.
        .concatWith(
            Mono.defer(
                () ->
                    doneEmitted.get()
                        ? Mono.empty()
                        : Mono.just(sse(DONE_PAYLOAD_FORMAT.formatted(questionNum)))))
        .onErrorResume(
            error -> {
              log.error("AI 인터뷰 스트림 중계 실패. meetingId={}, questionNum={}", meetingId, questionNum, error);
              return Flux.just(
                  sse(
                      ERROR_PAYLOAD_FORMAT.formatted(
                          ErrorCode.AI_SERVICE_ERROR.getCode(),
                          ErrorCode.AI_SERVICE_ERROR.getMessage())),
                  sse(DONE_PAYLOAD_FORMAT.formatted(questionNum)));
            });
  }

  /** 캐시된 질문을 원래 스트림과 동일한 형태(타자기 효과)로 재생한다. */
  private Flux<ServerSentEvent<String>> replayCachedQuestion(String questionText, int questionNum) {
    List<ServerSentEvent<String>> chunks = new ArrayList<>();
    for (int start = 0; start < questionText.length(); start += REPLAY_CHUNK_SIZE) {
      String chunk =
          questionText.substring(start, Math.min(start + REPLAY_CHUNK_SIZE, questionText.length()));
      chunks.add(sse("{\"chunk\":%s}".formatted(toJsonString(chunk))));
    }

    return Flux.fromIterable(chunks)
        .delayElements(REPLAY_CHUNK_DELAY)
        .concatWith(Mono.just(sse(DONE_PAYLOAD_FORMAT.formatted(questionNum))));
  }

  /** SSE data 페이로드에서 chunk 텍스트를 추출한다. */
  private Optional<String> extractChunk(String data) {
    try {
      JsonNode node = objectMapper.readTree(data);
      JsonNode chunk = node.get("chunk");
      return chunk == null || chunk.isNull() ? Optional.empty() : Optional.of(chunk.asText());
    } catch (Exception e) {
      log.debug("SSE 청크 파싱 실패 (중계는 계속): {}", data);
      return Optional.empty();
    }
  }

  private String toJsonString(String value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      return "\"\"";
    }
  }

  @Transactional
  public InterviewAnswerResponse submitAnswer(
      Long meetingId, Long userId, InterviewAnswerRequest request) {
    Meeting meeting = meetingService.getAccessibleMeeting(meetingId, userId);
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    preInterviewAnswerRepository
        .findByMeetingIdAndUserIdAndQuestionNum(meetingId, userId, request.questionNum())
        .ifPresentOrElse(
            existing -> existing.updateAnswer(request.questionText(), request.answerText()),
            () ->
                preInterviewAnswerRepository.save(
                    PreInterviewAnswer.builder()
                        .meeting(meeting)
                        .user(user)
                        .questionNum(request.questionNum())
                        .questionText(request.questionText())
                        .answerText(request.answerText())
                        .build()));

    if (meeting.getStatus() == MeetingStatus.BEFORE_INTERVIEW) {
      meeting.changeStatus(MeetingStatus.IN_PROGRESS);
    }

    // 질문 N 은 답변 1..N-1 을 컨텍스트로 생성되므로, 답변이 들어오면 이후 번호의 캐시는 낡은 것이 된다.
    questionCache.invalidateAfter(meetingId, userId, request.questionNum());

    boolean completed = request.questionNum() >= MeetingService.TOTAL_INTERVIEW_QUESTIONS;
    return new InterviewAnswerResponse(completed ? null : request.questionNum() + 1, completed);
  }

  private AiInterviewRequest buildContext(Long meetingId, Long userId, int questionNum) {
    Meeting meeting = meetingService.getAccessibleMeeting(meetingId, userId);

    List<AiInterviewRequest.PreviousAnswer> previousAnswers =
        preInterviewAnswerRepository
            .findAllByMeetingIdAndUserIdOrderByQuestionNumAsc(meetingId, userId)
            .stream()
            .map(
                answer ->
                    new AiInterviewRequest.PreviousAnswer(
                        answer.getQuestionNum(), answer.getQuestionText(), answer.getAnswerText()))
            .toList();

    List<String> previousMisunderstandings =
        meetingRepository
            .findPreviousMeetings(meeting.getProject().getId(), meeting.getMeetingAt())
            .stream()
            .findFirst()
            .flatMap(previous -> meetingResultRepository.findByMeetingId(previous.getId()))
            .map(result -> List.copyOf(result.getMisunderstandings()))
            .orElseGet(List::of);

    String participantName =
        userRepository.findById(userId).map(User::getName).orElse("참석자");

    return new AiInterviewRequest(
        meetingId,
        questionNum,
        MeetingService.TOTAL_INTERVIEW_QUESTIONS,
        participantName,
        meeting.getTitle(),
        meeting.getPurpose(),
        meeting.getAgendas().stream().map(MeetingAgenda::getContent).toList(),
        previousAnswers,
        previousMisunderstandings);
  }

  private static ServerSentEvent<String> sse(String data) {
    return ServerSentEvent.<String>builder().data(data).build();
  }
}
