package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;

import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMetricGroupKeys;

public class AddAccountIdToTimeSeriesMetricGroupMigration extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "timeSeriesMetricGroup";
  }

  @Override
  protected String getFieldName() {
    return TimeSeriesMetricGroupKeys.accountId;
  }
}
