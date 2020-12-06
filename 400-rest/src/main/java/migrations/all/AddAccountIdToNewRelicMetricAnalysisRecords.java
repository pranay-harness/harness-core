package migrations.all;

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
