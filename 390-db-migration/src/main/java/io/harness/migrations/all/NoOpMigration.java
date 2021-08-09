package io.harness.migrations.all;

import io.harness.migrations.Migration;

/**
 * Sometimes you add a migration, then revert it.
 * But the `schema` collection value in database would be updated.
 *
 * NoOp migration to handle such cases where `schema` value is ahead of MigrationList value.
 */
public class NoOpMigration implements Migration {
  @Override
  public void migrate() {}
}
