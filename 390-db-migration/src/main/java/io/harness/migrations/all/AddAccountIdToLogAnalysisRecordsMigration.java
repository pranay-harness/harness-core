/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import software.wings.service.impl.analysis.LogMLAnalysisRecord.LogMLAnalysisRecordKeys;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddAccountIdToLogAnalysisRecordsMigration extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "logAnalysisRecords";
  }

  @Override
  protected String getFieldName() {
    return LogMLAnalysisRecordKeys.accountId;
  }
}
