/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

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
