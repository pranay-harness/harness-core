/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import software.wings.sm.StateMachine.StateMachineKeys;

public class AddAccountIdToStateMachine extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "stateMachines";
  }

  @Override
  protected String getFieldName() {
    return StateMachineKeys.accountId;
  }
}
