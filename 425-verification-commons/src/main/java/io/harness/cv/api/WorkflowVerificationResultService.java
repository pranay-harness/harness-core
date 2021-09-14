/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cv.api;

import io.harness.beans.ExecutionStatus;
import io.harness.cv.WorkflowVerificationResult;

public interface WorkflowVerificationResultService {
  void addWorkflowVerificationResult(WorkflowVerificationResult workflowVerificationResult);

  void updateWorkflowVerificationResult(
      String stateExecutionId, boolean analyzed, ExecutionStatus executionStatus, String message);
}
