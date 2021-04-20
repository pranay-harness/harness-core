package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.analysis.MetricAnalysisRecord.MetricAnalysisRecordKeys;

@TargetModule(HarnessModule._390_DB_MIGRATION)
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
