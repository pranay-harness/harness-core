package io.harness.perpetualtask.remote;

import com.google.protobuf.Message;

import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.RemotePerpetualTaskClientContext;

public interface RemotePerpetualTaskServiceClient {
  Message getTaskParams(RemotePerpetualTaskClientContext clientContext);

  void onTaskStateChange(
      String taskId, PerpetualTaskResponse newPerpetualTaskResponse, PerpetualTaskResponse oldPerpetualTaskResponse);

  ValidationTaskDetails getValidationTask(RemotePerpetualTaskClientContext clientContext, String accountId);
}
