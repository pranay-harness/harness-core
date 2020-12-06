package migrations.all;

import software.wings.beans.baseline.WorkflowExecutionBaseline.WorkflowExecutionBaselineKeys;

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
