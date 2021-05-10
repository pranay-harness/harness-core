package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;

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
