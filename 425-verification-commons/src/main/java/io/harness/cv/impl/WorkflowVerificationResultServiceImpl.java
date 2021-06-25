package io.harness.cv.impl;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.beans.ExecutionStatus;
import io.harness.cv.WorkflowVerificationResult;
import io.harness.cv.WorkflowVerificationResult.WorkflowVerificationResultKeys;
import io.harness.cv.api.WorkflowVerificationResultService;
import io.harness.persistence.HPersistence;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class WorkflowVerificationResultServiceImpl implements WorkflowVerificationResultService {
  @Inject private HPersistence hPersistence;
  @Override
  public void addWorkflowVerificationResult(WorkflowVerificationResult workflowVerificationResult) {
    Preconditions.checkNotNull(workflowVerificationResult);
    hPersistence.save(workflowVerificationResult);
  }

  @Override
  public void updateWorkflowVerificationResult(
      String stateExecutionId, boolean analyzed, ExecutionStatus executionStatus, String message) {
    Preconditions.checkNotNull(stateExecutionId);
    Preconditions.checkNotNull(executionStatus);
    boolean rollback = analyzed && ExecutionStatus.FAILED.equals(executionStatus);
    hPersistence.update(hPersistence.createQuery(WorkflowVerificationResult.class, excludeAuthority)
                            .filter(WorkflowVerificationResultKeys.stateExecutionId, stateExecutionId),
        hPersistence.createUpdateOperations(WorkflowVerificationResult.class)
            .set(WorkflowVerificationResultKeys.executionStatus, executionStatus)
            .set(WorkflowVerificationResultKeys.analyzed, analyzed)
            .set(WorkflowVerificationResultKeys.rollback, rollback)
            .set(WorkflowVerificationResultKeys.message, message));
  }
}
