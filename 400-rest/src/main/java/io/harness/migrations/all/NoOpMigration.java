package io.harness.migrations.all;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;

/**
 * Sometimes you add a migration, then revert it.
 * But the `schema` collection value in database would be updated.
 *
 * NoOp migration to handle such cases where `schema` value is ahead of MigrationList value.
 */
@TargetModule(Module._390_DB_MIGRATION)
public class NoOpMigration implements Migration {
  @Override
  public void migrate() {}
}
