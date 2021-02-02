package io.harness.service.intfc;

import io.harness.beans.DelegateTask;
import io.harness.selection.log.BatchDelegateSelectionLog;

public interface DelegateTaskAssignService {
  boolean canAssign(BatchDelegateSelectionLog batch, String delegateId, DelegateTask task);

  boolean isWhitelisted(DelegateTask task, String delegateId);

  boolean shouldValidate(DelegateTask task, String delegateId);
}
