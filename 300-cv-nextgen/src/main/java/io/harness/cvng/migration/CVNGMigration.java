package io.harness.cvng.migration;

import io.harness.cvng.migration.service.MigrationChecklist;

public interface CVNGMigration extends MigrationChecklist {
  void migrate();
}
