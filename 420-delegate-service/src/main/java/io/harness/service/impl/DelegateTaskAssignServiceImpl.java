/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
