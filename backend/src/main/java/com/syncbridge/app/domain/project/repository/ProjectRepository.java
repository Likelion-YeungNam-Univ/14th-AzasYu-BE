package com.syncbridge.app.domain.project.repository;

import com.syncbridge.app.domain.project.entity.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

  Optional<Project> findByJoinCode(String joinCode);

  boolean existsByJoinCode(String joinCode);

  @Query(
      """
      select p from Project p
        join ProjectMember pm on pm.project = p
        left join fetch p.createdBy
      where pm.user.id = :userId
      order by p.createdAt desc
      """)
  List<Project> findAllByMemberUserId(@Param("userId") Long userId);
}
