package io.harness.ng.core.services;

import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.entities.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;

public interface ProjectService {
  Project create(String accountIdentifier, String orgIdentifier, ProjectDTO project);

  Optional<Project> get(String accountIdentifier, String orgIdentifier, String identifier);

  Project update(String accountIdentifier, String orgIdentifier, String identifier, ProjectDTO project);

  Page<Project> list(String accountIdentifier, Pageable pageable, ProjectFilterDTO projectFilterDTO);

  Page<Project> list(Criteria criteria, Pageable pageable);

  boolean delete(String accountIdentifier, String orgIdentifier, String identifier, Long version);
}
