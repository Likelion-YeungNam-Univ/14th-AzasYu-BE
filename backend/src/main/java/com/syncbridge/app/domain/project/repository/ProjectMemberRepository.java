package com.syncbridge.app.domain.project.repository;

import com.syncbridge.app.domain.project.dto.ProjectMemberCount;
import com.syncbridge.app.domain.project.entity.Project;
import com.syncbridge.app.domain.project.entity.ProjectMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

  boolean existsByProjectIdAndUserId(Long projectId, Long userId);

  long countByProjectId(Long projectId);

  List<ProjectMember> findAllByProject(Project project);

  @Query(
      """
      select new com.syncbridge.app.domain.project.dto.ProjectMemberCount(pm.project.id, count(pm))
      from ProjectMember pm
      where pm.project.id in :projectIds
      group by pm.project.id
      """)
  List<ProjectMemberCount> countByProjectIds(@Param("projectIds") List<Long> projectIds);
}
