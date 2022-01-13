/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
