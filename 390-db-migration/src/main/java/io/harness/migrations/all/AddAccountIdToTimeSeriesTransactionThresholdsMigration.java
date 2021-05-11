package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;

import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds.TimeSeriesMLTransactionThresholdKeys;

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
