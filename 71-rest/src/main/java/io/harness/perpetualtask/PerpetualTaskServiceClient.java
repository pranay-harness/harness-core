package io.harness.perpetualtask;

import com.google.protobuf.Message;

import io.harness.beans.DelegateTask;

/**
 * Used on the manager side to handle CRUD of a specific type of perpetual tasks.
 * @param <T> The params type of the perpetual task type being managed.
 */
public interface PerpetualTaskServiceClient<T extends PerpetualTaskClientParams> {
  String create(String accountId, T clientParams);

  boolean reset(String accountId, String taskId);

  boolean delete(String accountId, String taskId);

  Message getTaskParams(PerpetualTaskClientContext clientContext);

  DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId);
}
