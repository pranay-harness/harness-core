package io.harness.ng.core.remote.server.grpc.perpetualtask;

import com.google.protobuf.Message;

import io.harness.perpetualtask.PerpetualTaskExecutionResponse;
import io.harness.perpetualtask.RemotePerpetualTaskClientContext;
import io.harness.perpetualtask.remote.ValidationTaskDetails;

public interface RemotePerpetualTaskServiceClientManager {
  ValidationTaskDetails getValidationTask(
      String taskType, RemotePerpetualTaskClientContext remotePerpetualTaskClientContext, String accountId);

  Message getTaskParams(String taskType, RemotePerpetualTaskClientContext remotePerpetualTaskClientContext);

  void reportPerpetualTaskStateChange(String taskId, String taskType, PerpetualTaskExecutionResponse newResponse,
      PerpetualTaskExecutionResponse oldResponse);
}
