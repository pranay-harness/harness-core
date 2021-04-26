package io.harness.migration.service;

import io.harness.migration.beans.NGMigrationConfiguration;

public interface NGMigrationService {
  void runMigrations(NGMigrationConfiguration configuration);
}
