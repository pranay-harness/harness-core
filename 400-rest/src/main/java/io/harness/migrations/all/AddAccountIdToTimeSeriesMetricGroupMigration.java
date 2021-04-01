package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMetricGroupKeys;

@TargetModule(HarnessModule._390_DB_MIGRATION)
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
