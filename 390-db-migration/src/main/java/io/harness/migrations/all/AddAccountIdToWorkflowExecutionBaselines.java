/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

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
