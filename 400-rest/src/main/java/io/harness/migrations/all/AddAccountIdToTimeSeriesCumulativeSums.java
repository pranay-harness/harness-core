package io.harness.migrations.all;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._390_DB_MIGRATION)
public class AddAccountIdToTimeSeriesCumulativeSums extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "timeSeriesCumulativeSums";
  }

  @Override
  protected String getFieldName() {
    return "accountId";
  }
}
