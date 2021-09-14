/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import software.wings.beans.infrastructure.CloudFormationRollbackConfig.CloudFormationRollbackConfigKeys;

public class AddAccountIdToCloudFormationRollBackConfig extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "cloudFormationRollbackConfig";
  }

  @Override
  protected String getFieldName() {
    return CloudFormationRollbackConfigKeys.accountId;
  }
}
