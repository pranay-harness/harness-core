package io.harness.migrations.all;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.sm.StateMachine.StateMachineKeys;

@TargetModule(Module._390_DB_MIGRATION)
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
