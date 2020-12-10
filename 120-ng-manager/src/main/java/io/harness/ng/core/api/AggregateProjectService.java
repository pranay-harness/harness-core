package io.harness.ng.core.api;

import io.harness.ng.core.dto.ProjectAggregateDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AggregateProjectService {
  ProjectAggregateDTO getProjectAggregateDTO(String accountIdentifier, String orgIdentifier, String identifier);

  Page<ProjectAggregateDTO> listProjectAggregateDTO(
      String accountIdentifier, Pageable pageable, ProjectFilterDTO projectFilterDTO);
}
