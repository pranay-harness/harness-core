package io.harness.migrations.all;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._390_DB_MIGRATION)
public class AddAccountIdToTimeSeriesAnomaliesRecordMigration extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "timeSeriesAnomaliesRecords";
  }

  @Override
  protected String getFieldName() {
    return "accountId";
  }
}
