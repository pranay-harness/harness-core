package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.analysis.TimeSeriesRiskSummary.TimeSeriesRiskSummaryKeys;

@TargetModule(HarnessModule._390_DB_MIGRATION)
public class AddAccountIdToTimeSeriesRiskSummary extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "timeSeriesRiskSummary";
  }

  @Override
  protected String getFieldName() {
    return TimeSeriesRiskSummaryKeys.accountId;
  }
}
