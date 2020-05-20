package io.harness.cdng.core.services.api;

import io.harness.cdng.core.entities.Project;

public interface ProjectService {
  Project save(Project project);

  Project get(String id);
}
