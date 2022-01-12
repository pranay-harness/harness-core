/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import software.wings.service.impl.analysis.TimeSeriesRiskSummary.TimeSeriesRiskSummaryKeys;

public class AddAccountIdToTimeSeriesRiskSummary extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "timeSeriesRiskSummary";
  }

  @Override
  protected String getFieldName() {
    return TimeSeriesRiskSummaryKeys.accountId;
  }
}
