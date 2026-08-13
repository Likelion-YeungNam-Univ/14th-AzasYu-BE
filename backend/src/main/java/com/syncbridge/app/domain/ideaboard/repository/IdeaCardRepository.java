package com.syncbridge.app.domain.ideaboard.repository;

import com.syncbridge.app.domain.ideaboard.entity.IdeaCard;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdeaCardRepository extends JpaRepository<IdeaCard, Long> {

  @Query(
      """
      select i from IdeaCard i
        left join fetch i.user
      where i.meeting.id = :meetingId
      order by i.createdAt asc, i.id asc
      """)
  List<IdeaCard> findAllByMeetingIdOrderByCreatedAt(@Param("meetingId") Long meetingId);
}
