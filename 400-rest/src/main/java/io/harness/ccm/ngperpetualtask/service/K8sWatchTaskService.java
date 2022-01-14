package io.harness.ccm.ngperpetualtask.service;

import io.harness.ccm.K8sEventCollectionBundle;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

public interface K8sWatchTaskService {
  String create(String accountId, K8sEventCollectionBundle bundle);

  boolean resetTask(String accountId, String taskId, K8sEventCollectionBundle bundle);

  boolean delete(String accountId, String taskId);

  PerpetualTaskRecord getStatus(String taskId);
}
