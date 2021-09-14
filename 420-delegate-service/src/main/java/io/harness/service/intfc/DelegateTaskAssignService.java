/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.intfc;

import io.harness.beans.DelegateTask;
import io.harness.selection.log.BatchDelegateSelectionLog;

public interface DelegateTaskAssignService {
  boolean canAssign(BatchDelegateSelectionLog batch, String delegateId, DelegateTask task);

  boolean isWhitelisted(DelegateTask task, String delegateId);

  boolean shouldValidate(DelegateTask task, String delegateId);
}
