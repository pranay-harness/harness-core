/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import software.wings.beans.Log.LogKeys;

public class AddAccountIdToCommandLogs extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "commandLogs";
  }

  @Override
  protected String getFieldName() {
    return LogKeys.accountId;
  }
}
