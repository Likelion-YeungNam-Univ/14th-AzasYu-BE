package com.syncbridge.app.domain.ideaboard.service;

import com.syncbridge.app.domain.auth.entity.User;
import com.syncbridge.app.domain.auth.repository.UserRepository;
import com.syncbridge.app.domain.ideaboard.dto.IdeaCreateRequest;
import com.syncbridge.app.domain.ideaboard.dto.IdeaCreateResponse;
import com.syncbridge.app.domain.ideaboard.dto.IdeaResponseDto;
import com.syncbridge.app.domain.ideaboard.entity.IdeaCard;
import com.syncbridge.app.domain.ideaboard.repository.IdeaCardRepository;
import com.syncbridge.app.domain.meeting.entity.Meeting;
import com.syncbridge.app.domain.meeting.service.MeetingService;
import com.syncbridge.app.global.error.CustomException;
import com.syncbridge.app.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아이디어 보드 서비스.
 *
 * <p><b>소프트 익명성:</b> 작성자 user_id 는 DB(idea_card.user_id)에 그대로 저장해 개인 성과 반영에 대비하고, 응답 DTO
 * ({@link IdeaResponseDto})로 변환하는 시점에만 "익명"으로 마스킹한다. 컨트롤러/서비스 어디에서도 작성자 식별 정보를 반환하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IdeaService {

  /** AI 가 감지한 오해 리스크 카드 접두사. */
  public static final String AI_RISK_PREFIX = "[AI 리스크 감지] ";

  private final IdeaCardRepository ideaCardRepository;
  private final UserRepository userRepository;
  private final MeetingService meetingService;

  public List<IdeaResponseDto> getIdeas(Long meetingId, Long userId) {
    meetingService.getAccessibleMeeting(meetingId, userId);
    return ideaCardRepository.findAllByMeetingIdOrderByCreatedAt(meetingId).stream()
        .map(IdeaResponseDto::from)
        .toList();
  }

  @Transactional
  public IdeaCreateResponse createIdea(Long meetingId, Long userId, IdeaCreateRequest request) {
    Meeting meeting = meetingService.getAccessibleMeeting(meetingId, userId);
    User author =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    // user_id 는 저장하되(성과 반영 대비), 조회 응답에서는 "익명"으로만 노출된다.
    IdeaCard saved =
        ideaCardRepository.save(IdeaCard.ofMember(meeting, author, request.content().trim()));
    return IdeaCreateResponse.success(saved.getId());
  }

  /** AI 분석이 감지한 오해 리스크를 보드에 카드로 게시한다 (선순환 구조). */
  @Transactional
  public void createAiRiskCards(Meeting meeting, List<String> misunderstandings) {
    if (misunderstandings == null || misunderstandings.isEmpty()) {
      return;
    }
    List<IdeaCard> cards =
        misunderstandings.stream()
            .filter(risk -> risk != null && !risk.isBlank())
            .map(risk -> IdeaCard.ofAi(meeting, AI_RISK_PREFIX + risk.trim()))
            .toList();
    ideaCardRepository.saveAll(cards);
  }
}
