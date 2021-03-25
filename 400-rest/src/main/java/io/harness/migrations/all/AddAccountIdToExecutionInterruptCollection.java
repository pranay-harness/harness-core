package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.sm.ExecutionInterrupt.ExecutionInterruptKeys;

@TargetModule(HarnessModule._390_DB_MIGRATION)
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
