package io.harness.migrations;

/**
 * This migration should be used by classes where we want to add seed data to an installation.
 */
import io.harness.annotations.dev.HarnessModule;
public interface SeedDataMigration {
  void migrate();
}
