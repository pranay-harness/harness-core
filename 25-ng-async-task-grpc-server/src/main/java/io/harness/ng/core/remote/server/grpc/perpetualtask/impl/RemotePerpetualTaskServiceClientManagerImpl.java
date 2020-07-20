package io.harness.ng.core.remote.server.grpc.perpetualtask.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Message;

import io.harness.exception.NoResultFoundException;
import io.harness.ng.core.remote.server.grpc.perpetualtask.RemotePerpetualTaskServiceClientManager;
import io.harness.perpetualtask.PerpetualTaskExecutionResponse;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.RemotePerpetualTaskClientContext;
import io.harness.perpetualtask.remote.RemotePerpetualTaskServiceClient;
import io.harness.perpetualtask.remote.RemotePerpetualTaskServiceClientRegistry;
import io.harness.perpetualtask.remote.ValidationTaskDetails;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))

@Slf4j
public class RemotePerpetualTaskServiceClientManagerImpl implements RemotePerpetualTaskServiceClientManager {
  private final RemotePerpetualTaskServiceClientRegistry clientRegistry;

  @Override
  public ValidationTaskDetails getValidationTask(
      String taskType, RemotePerpetualTaskClientContext remotePerpetualTaskClientContext, String accountId) {
    final RemotePerpetualTaskServiceClient pTaskServiceClient = getPTaskServiceClient(taskType);
    return pTaskServiceClient.getValidationTask(remotePerpetualTaskClientContext, accountId);
  }

  @Override
  public Message getTaskParams(String taskType, RemotePerpetualTaskClientContext remotePerpetualTaskClientContext) {
    final RemotePerpetualTaskServiceClient pTaskServiceClient = getPTaskServiceClient(taskType);
    return pTaskServiceClient.getTaskParams(remotePerpetualTaskClientContext);
  }

  @Override
  public void reportPerpetualTaskStateChange(String taskId, String taskType, PerpetualTaskExecutionResponse newResponse,
      PerpetualTaskExecutionResponse oldResponse) {
    final RemotePerpetualTaskServiceClient pTaskServiceClient = getPTaskServiceClient(taskType);
    pTaskServiceClient.onTaskStateChange(taskId, buildTaskResponse(newResponse), buildTaskResponse(oldResponse));
  }

  private PerpetualTaskResponse buildTaskResponse(PerpetualTaskExecutionResponse executionResponse) {
    return PerpetualTaskResponse.builder()
        .responseCode(executionResponse.getResponseCode())
        .responseMessage(executionResponse.getResponseMessage())
        .perpetualTaskState(PerpetualTaskState.valueOf(executionResponse.getTaskState()))
        .build();
  }

  private RemotePerpetualTaskServiceClient getPTaskServiceClient(String taskType) {
    return clientRegistry.getClient(taskType).orElseThrow(
        ()
            -> NoResultFoundException.newBuilder()
                   .message("no remote service client found for taskType =" + taskType)
                   .build());
  }
}
