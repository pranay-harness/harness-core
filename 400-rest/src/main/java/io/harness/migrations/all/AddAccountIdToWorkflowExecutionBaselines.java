package io.harness.migrations.all;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.baseline.WorkflowExecutionBaseline.WorkflowExecutionBaselineKeys;

@TargetModule(Module._390_DB_MIGRATION)
public class AddAccountIdToWorkflowExecutionBaselines extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "workflowExecutionBaselines";
  }

  @Override
  protected String getFieldName() {
    return WorkflowExecutionBaselineKeys.accountId;
  }
}
