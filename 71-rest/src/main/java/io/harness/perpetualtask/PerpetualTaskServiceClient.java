package io.harness.perpetualtask;

import com.google.protobuf.Message;

import io.harness.beans.DelegateTask;

/**
 * Used on the manager side to handle CRUD of a specific type of perpetual tasks.
 * @param <T> The params type of the perpetual task type being managed.
 */
public interface PerpetualTaskServiceClient<T extends PerpetualTaskClientParams> {
  String create(String accountId, T clientParams);

  Message getTaskParams(PerpetualTaskClientContext clientContext);

  void onTaskStateChange(
      String taskId, PerpetualTaskResponse newPerpetualTaskResponse, PerpetualTaskResponse oldPerpetualTaskResponse);

  DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId);
}
