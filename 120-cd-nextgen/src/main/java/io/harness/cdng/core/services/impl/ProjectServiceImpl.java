package io.harness.cdng.core.services.impl;

import com.google.inject.Inject;

import io.harness.cdng.core.entities.Project;
import io.harness.cdng.core.services.api.ProjectService;
import io.harness.persistence.HPersistence;

public class ProjectServiceImpl implements ProjectService {
  private final HPersistence persistence;

  @Inject
  public ProjectServiceImpl(HPersistence persistence) {
    this.persistence = persistence;
  }

  @Override
  public Project save(Project project) {
    String id = persistence.save(project);
    return get(id);
  }

  @Override
  public Project get(String id) {
    return persistence.get(Project.class, id);
  }
}
