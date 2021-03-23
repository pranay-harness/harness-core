package io.harness.accesscontrol.migrations.services;

import io.harness.accesscontrol.migrations.models.Migration;

public interface MigrationService {
  Migration save(Migration migration);
}
