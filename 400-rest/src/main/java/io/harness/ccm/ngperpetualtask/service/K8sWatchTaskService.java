/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.ngperpetualtask.service;

import io.harness.ccm.K8sEventCollectionBundle;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

public interface K8sWatchTaskService {
  String create(String accountId, K8sEventCollectionBundle bundle);

  boolean resetTask(String accountId, String taskId, K8sEventCollectionBundle bundle);

  boolean delete(String accountId, String taskId);

  PerpetualTaskRecord getStatus(String taskId);
}
