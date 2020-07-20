package io.harness.cdng.environment;

import io.harness.cdng.environment.beans.Environment;

import javax.annotation.Nonnull;

public interface EnvironmentService {
  void upsert(@Nonnull Environment environment);

  Environment getEnvironment(@Nonnull String accountId, String orgId, String projectId, @Nonnull String identifier);
}
