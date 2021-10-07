package io.harness.ng.core.services;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.entities.Project;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface ProjectService {
  Project create(String accountIdentifier, String orgIdentifier, ProjectDTO project);

  Optional<Project> get(String accountIdentifier, String orgIdentifier, String identifier);

  Project update(String accountIdentifier, String orgIdentifier, String identifier, ProjectDTO project);

  PageResponse<ProjectDTO> listProjectsForUser(String userId, String accountId, PageRequest pageRequest);

  List<ProjectDTO> listProjectsForUser(String userId, String accountId);

  Integer accessibleProjectsCount(String userId, String accountId, long startInterval, long endInterval);

    /**
     * Use this method with caution, verify that the pageable sort is able to make use of the indexes.
     */
  Page<Project> listPermittedProjects(String accountIdentifier, Pageable pageable, ProjectFilterDTO projectFilterDTO);

  /**
   * Use this method with caution, verify that the criteria and pageable sort is able to make use of the indexes.
   */
  Page<Project> list(Criteria criteria, Pageable pageable);

  /**
   * Use this method with caution, verify that the criteria is able to make use of the indexes.
   */
  List<Project> list(Criteria criteria);

  boolean delete(String accountIdentifier, String orgIdentifier, String identifier, Long version);

  boolean restore(String accountIdentifier, String orgIdentifier, String identifier);

  Map<String, Integer> getProjectsCountPerOrganization(String accountIdentifier, List<String> orgIdentifiers);

  Long countProjects(String accountIdenifier);
}
