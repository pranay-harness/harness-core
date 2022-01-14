package io.harness.migrations.all;

import software.wings.sm.ExecutionInterrupt.ExecutionInterruptKeys;

public class AddAccountIdToExecutionInterruptCollection extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "executionInterrupts";
  }

  @Override
  protected String getFieldName() {
    return ExecutionInterruptKeys.accountId;
  }
}
