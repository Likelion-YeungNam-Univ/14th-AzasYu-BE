package com.syncbridge.app.domain.result.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.syncbridge.app.domain.result.entity.ActionItem;
import java.util.List;

/** FastAPI(`POST /ai/analysis/summarize`) 응답 매핑. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiAnalysisResponse(
    String purpose,
    List<String> mainDiscussions,
    List<String> decisions,
    List<ActionItem> actionItems,
    List<String> misunderstandings) {}
