package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;

import software.wings.service.impl.analysis.MetricAnalysisRecord.MetricAnalysisRecordKeys;

public class AddAccountIdToTimeSeriesAnalysisRecords extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "timeSeriesAnalysisRecords";
  }

  @Override
  protected String getFieldName() {
    return MetricAnalysisRecordKeys.accountId;
  }
}
