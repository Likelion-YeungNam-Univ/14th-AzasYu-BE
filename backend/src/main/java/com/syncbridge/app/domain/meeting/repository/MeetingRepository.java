package com.syncbridge.app.domain.meeting.repository;

import com.syncbridge.app.domain.meeting.entity.Meeting;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

  List<Meeting> findAllByProjectIdOrderByMeetingAtDesc(Long projectId);

  List<Meeting> findAllByProjectIdIn(List<Long> projectIds);

  @Query(
      """
      select m from Meeting m
        join fetch m.project p
        left join fetch p.createdBy
      where m.id = :meetingId
      """)
  Optional<Meeting> findWithProjectById(@Param("meetingId") Long meetingId);

  /** 직전 회의(현재 회의보다 이전에 열린 같은 프로젝트의 가장 최근 회의). */
  @Query(
      """
      select m from Meeting m
      where m.project.id = :projectId
        and m.meetingAt < :meetingAt
      order by m.meetingAt desc
      """)
  List<Meeting> findPreviousMeetings(
      @Param("projectId") Long projectId,
      @Param("meetingAt") java.time.LocalDateTime meetingAt);
}
