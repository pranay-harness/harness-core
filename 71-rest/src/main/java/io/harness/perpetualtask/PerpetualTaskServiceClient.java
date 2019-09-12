package io.harness.perpetualtask;

import com.google.protobuf.Message;

import io.harness.beans.DelegateTask;

public interface PerpetualTaskServiceClient<T> {
  String create(String accountId, T clientParams);

  boolean delete(String accountId, String taskId);

  Message getTaskParams(PerpetualTaskClientContext clientContext);

  DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId);
}
