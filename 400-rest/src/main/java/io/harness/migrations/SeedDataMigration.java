package io.harness.migrations;

/**
 * This migration should be used by classes where we want to add seed data to an installation.
 */
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._390_DB_MIGRATION)
public interface SeedDataMigration {
  void migrate();
}
