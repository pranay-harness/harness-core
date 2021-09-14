/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.impl;

import io.harness.beans.DelegateTask;
import io.harness.selection.log.BatchDelegateSelectionLog;
import io.harness.service.intfc.DelegateTaskAssignService;

public class DelegateTaskAssignServiceImpl implements DelegateTaskAssignService {
  public boolean canAssign(BatchDelegateSelectionLog batch, String delegateId, DelegateTask task) {
    return false;
  }

  public boolean isWhitelisted(DelegateTask task, String delegateId) {
    return false;
  }

  public boolean shouldValidate(DelegateTask task, String delegateId) {
    return false;
  }
}
