package migrations.all;

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
