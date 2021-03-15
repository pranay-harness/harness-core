package io.harness.cvng.migration.list;

import io.harness.cvng.migration.CVNGMigration;
import io.harness.cvng.migration.beans.ChecklistItem;

public class CVNGBaseMigration implements CVNGMigration {
  @Override
  public void migrate() {}

  @Override
  public ChecklistItem whatHappensOnRollback() {
    return ChecklistItem.NA;
  }

  @Override
  public ChecklistItem whatHappensIfOldVersionIteratorPicksMigratedEntity() {
    return ChecklistItem.NA;
  }
}
