/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import software.wings.service.impl.analysis.LogDataRecord.LogDataRecordKeys;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddAccountIdToLogDataRecordsMigration extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "logDataRecords";
  }

  @Override
  protected String getFieldName() {
    return LogDataRecordKeys.accountId;
  }
}
