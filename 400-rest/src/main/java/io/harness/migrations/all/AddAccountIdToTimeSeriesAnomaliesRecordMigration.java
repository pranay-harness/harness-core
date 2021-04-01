package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._390_DB_MIGRATION)
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
