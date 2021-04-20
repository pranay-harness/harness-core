package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.analysis.TimeSeriesMLScores.TimeSeriesMLScoresKeys;

@TargetModule(HarnessModule._390_DB_MIGRATION)
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
