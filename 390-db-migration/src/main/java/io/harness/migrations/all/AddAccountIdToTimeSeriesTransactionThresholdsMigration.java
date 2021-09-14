/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds.TimeSeriesMLTransactionThresholdKeys;

public class AddAccountIdToTimeSeriesTransactionThresholdsMigration
    extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "timeseriesTransactionThresholds";
  }

  @Override
  protected String getFieldName() {
    return TimeSeriesMLTransactionThresholdKeys.accountId;
  }
}
