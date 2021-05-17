package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;

import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisRecordKeys;

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
