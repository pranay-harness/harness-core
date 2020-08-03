package io.harness.ng.core.services.api;

import io.harness.ng.core.entities.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.TextCriteria;

import java.util.Optional;

public interface ProjectService {
  Project create(Project project);

  Optional<Project> get(String orgIdentifier, String projectIdentifier);

  Project update(Project project);

  Page<Project> list(Criteria criteria, Pageable pageable);

  Page<Project> list(TextCriteria textCriteria, Criteria criteria, Pageable pageable);

  boolean delete(String orgIdentifier, String projectIdentifier);
}