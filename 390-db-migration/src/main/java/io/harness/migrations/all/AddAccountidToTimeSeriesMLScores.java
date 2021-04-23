package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;

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
