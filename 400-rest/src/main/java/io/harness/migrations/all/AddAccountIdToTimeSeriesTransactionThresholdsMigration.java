package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds.TimeSeriesMLTransactionThresholdKeys;

@TargetModule(HarnessModule._390_DB_MIGRATION)
public class AddAccountIdToTimeSeriesTransactionThresholdsMigration
    extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "timeseriesTransactionThresholds";
  }

  @Override
  protected String getFieldName() {
    return TimeSeriesMLTransactionThresholdKeys.accountId;
  }
}
