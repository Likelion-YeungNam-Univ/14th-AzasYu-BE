package com.syncbridge.app.domain.result.service;

import com.syncbridge.app.domain.result.dto.AiAnalysisResponse;
import com.syncbridge.app.global.error.CustomException;
import com.syncbridge.app.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** FastAPI 회의록 분석 API 호출 클라이언트. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAnalysisClient {

  private static final String SUMMARIZE_PATH = "/ai/analysis/summarize";

  private final WebClient aiServiceWebClient;

  /**
   * 회의록(파일 또는 원문 텍스트)을 FastAPI 로 전달해 구조화된 분석 결과를 받는다.
   *
   * <p>비동기 워커 스레드에서 호출되므로 {@code block()} 으로 결과를 기다린다.
   */
  public AiAnalysisResponse summarize(
      FileStorageService.StoredFile file,
      String rawText,
      String meetingTitle,
      String purpose,
      List<String> agendas,
      List<String> participantNames) {

    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    if (file != null) {
      builder
          .part(
              "file",
              new ByteArrayResource(file.bytes()) {
                @Override
                public String getFilename() {
                  return file.originalFilename();
                }
              })
          .contentType(
              file.contentType() == null
                  ? MediaType.APPLICATION_OCTET_STREAM
                  : MediaType.parseMediaType(file.contentType()));
    }
    if (rawText != null && !rawText.isBlank()) {
      builder.part("rawText", rawText);
    }
    builder.part("meetingTitle", meetingTitle == null ? "" : meetingTitle);
    builder.part("purpose", purpose == null ? "" : purpose);
    builder.part("agendas", String.join("\n", agendas == null ? List.of() : agendas));
    builder.part(
        "participants", String.join(",", participantNames == null ? List.of() : participantNames));

    try {
      return aiServiceWebClient
          .post()
          .uri(SUMMARIZE_PATH)
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(BodyInserters.fromMultipartData(builder.build()))
          .retrieve()
          .bodyToMono(AiAnalysisResponse.class)
          .block();
    } catch (WebClientResponseException e) {
      log.error("AI 분석 API 오류. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new CustomException(ErrorCode.AI_SERVICE_ERROR);
    } catch (RuntimeException e) {
      log.error("AI 분석 API 호출 실패", e);
      throw new CustomException(ErrorCode.AI_SERVICE_ERROR);
    }
  }
}
