package io.harness;

import static org.apache.commons.collections4.MapUtils.emptyIfNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;

import io.harness.delegate.AccountId;
import io.harness.delegate.SendTaskAsyncRequest;
import io.harness.delegate.SendTaskAsyncResponse;
import io.harness.delegate.SendTaskRequest;
import io.harness.delegate.SendTaskResponse;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.delegate.TaskType;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.grpc.ManagerDelegateGrpcClient;
import io.harness.serializer.KryoSerializer;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class ManagerDelegateServiceDriver {
  private final KryoSerializer kryoSerializer;
  private final ManagerDelegateGrpcClient managerDelegateGrpcClient;

  @Inject
  public ManagerDelegateServiceDriver(
      ManagerDelegateGrpcClient managerDelegateGrpcClient, KryoSerializer kryoSerializer) {
    this.managerDelegateGrpcClient = managerDelegateGrpcClient;
    this.kryoSerializer = kryoSerializer;
  }

  public SendTaskResponse sendTask(String accountId, Map<String, String> setupAbstractions, TaskData taskData) {
    SendTaskRequest request = buildSendTaskRequest(accountId, setupAbstractions, taskData);
    return managerDelegateGrpcClient.sendTask(request);
  }

  public String sendTaskAsync(String accountId, Map<String, String> setupAbstractions, TaskData taskData) {
    SendTaskAsyncRequest request = buildSendTaskAsyncRequest(accountId, emptyIfNull(setupAbstractions), taskData);
    SendTaskAsyncResponse response = managerDelegateGrpcClient.sendTaskAsync(request);
    return response.getTaskId().getId();
  }

  public io.harness.delegate.AbortTaskResponse abortTask(io.harness.delegate.AbortTaskRequest request) {
    return managerDelegateGrpcClient.abortTask(request);
  }

  private SendTaskRequest buildSendTaskRequest(
      String accountId, Map<String, String> setupAbstractions, TaskData taskData) {
    TaskDetails taskDetails = buildTaskDetails(taskData);
    return SendTaskRequest.newBuilder()
        .setAccountId(AccountId.newBuilder().setId(accountId).build())
        .setSetupAbstractions(TaskSetupAbstractions.newBuilder().putAllValues(setupAbstractions).build())
        .setDetails(taskDetails)
        .build();
  }

  private SendTaskAsyncRequest buildSendTaskAsyncRequest(
      String accountId, Map<String, String> setupAbstractions, TaskData taskData) {
    TaskDetails taskDetails = buildTaskDetails(taskData);
    return SendTaskAsyncRequest.newBuilder()
        .setAccountId(AccountId.newBuilder().setId(accountId).build())
        .setSetupAbstractions(TaskSetupAbstractions.newBuilder().putAllValues(setupAbstractions).build())
        .setDetails(taskDetails)
        .build();
  }

  private TaskDetails buildTaskDetails(TaskData taskData) {
    return TaskDetails.newBuilder()
        .setType(TaskType.newBuilder().setType(taskData.getTaskType()).build())
        .setKryoParameters(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(getTaskParameter(taskData))))
        .putAllExpressions(taskData.getExpressions() == null ? new HashMap<>() : taskData.getExpressions())
        .setExpressionFunctorToken(taskData.getExpressionFunctorToken())
        .setExecutionTimeout(Duration.newBuilder().setSeconds(taskData.getTimeout() * 1000).build())
        .build();
  }

  private TaskParameters getTaskParameter(TaskData taskData) {
    Object[] parameters = taskData.getParameters();
    if (parameters.length == 1 && parameters[0] instanceof TaskParameters) {
      return (TaskParameters) parameters[0];
    }
    throw new InvalidRequestException("Only Supported for task using task parameters");
  }
}
