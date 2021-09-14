/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

public class AddAccountIdToTimeSeriesCumulativeSums extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "timeSeriesCumulativeSums";
  }

  @Override
  protected String getFieldName() {
    return "accountId";
  }
}
