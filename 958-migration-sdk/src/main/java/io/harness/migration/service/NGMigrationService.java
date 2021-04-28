package io.harness.migration.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.beans.NGMigrationConfiguration;

@OwnedBy(DX)
public interface NGMigrationService {
  void runMigrations(NGMigrationConfiguration configuration);
}
