package io.harness.ng.accesscontrol.migrations.services;

import io.harness.ng.accesscontrol.migrations.models.Migration;

public interface MigrationService {
  Migration save(Migration migration);
}
