package com.syncbridge.app.domain.meeting.repository;

import com.syncbridge.app.domain.meeting.entity.MeetingParticipant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

  long countByMeetingId(Long meetingId);

  boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

  List<MeetingParticipant> findAllByMeetingId(Long meetingId);
}
