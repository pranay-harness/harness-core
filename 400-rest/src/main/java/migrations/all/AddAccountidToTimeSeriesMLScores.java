package migrations.all;

import software.wings.service.impl.analysis.TimeSeriesMLScores.TimeSeriesMLScoresKeys;

public class AddAccountidToTimeSeriesMLScores extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "timeSeriesMLScores";
  }

  @Override
  protected String getFieldName() {
    return TimeSeriesMLScoresKeys.accountId;
  }
}
