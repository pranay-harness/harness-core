package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
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
