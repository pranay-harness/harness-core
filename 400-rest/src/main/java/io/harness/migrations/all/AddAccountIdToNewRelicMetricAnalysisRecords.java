package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisRecordKeys;

@TargetModule(HarnessModule._390_DB_MIGRATION)
public class AddAccountIdToNewRelicMetricAnalysisRecords extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "newRelicMetricAnalysisRecords";
  }

  @Override
  protected String getFieldName() {
    return NewRelicMetricAnalysisRecordKeys.accountId;
  }
}
