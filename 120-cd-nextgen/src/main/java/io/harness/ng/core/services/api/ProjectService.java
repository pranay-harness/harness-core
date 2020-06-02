package io.harness.ng.core.services.api;

import io.harness.ng.core.entities.Project;

import java.util.List;
import java.util.Optional;

public interface ProjectService {
  Project create(Project project);

  Optional<Project> get(String projectId);

  Project update(Project project);

  List<Project> getAll();
}
