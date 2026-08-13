package com.syncbridge.app.domain.result.repository;

import com.syncbridge.app.domain.result.entity.MeetingResult;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingResultRepository extends JpaRepository<MeetingResult, Long> {

  Optional<MeetingResult> findByMeetingId(Long meetingId);
}
