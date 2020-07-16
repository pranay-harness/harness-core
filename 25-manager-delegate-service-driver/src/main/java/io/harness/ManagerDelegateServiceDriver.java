package io.harness;

import static org.apache.commons.collections4.MapUtils.emptyIfNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;

import io.harness.delegate.AbortTaskRequest;
import io.harness.delegate.AbortTaskResponse;
import io.harness.delegate.NgAccountId;
import io.harness.delegate.NgTaskDetails;
import io.harness.delegate.NgTaskExecutionStage;
import io.harness.delegate.NgTaskId;
import io.harness.delegate.NgTaskSetupAbstractions;
import io.harness.delegate.NgTaskType;
import io.harness.delegate.SendTaskAsyncRequest;
import io.harness.delegate.SendTaskAsyncResponse;
import io.harness.delegate.SendTaskRequest;
import io.harness.delegate.SendTaskResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NoResultFoundException;
import io.harness.grpc.ManagerDelegateGrpcClient;
import io.harness.perpetualtask.CreateRemotePerpetualTaskRequest;
import io.harness.perpetualtask.DeleteRemotePerpetualTaskRequest;
import io.harness.perpetualtask.RemotePerpetualTaskClientContext;
import io.harness.perpetualtask.RemotePerpetualTaskSchedule;
import io.harness.perpetualtask.ResetRemotePerpetualTaskRequest;
import io.harness.serializer.KryoSerializer;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Singleton
@Slf4j
public class ManagerDelegateServiceDriver {
  private final KryoSerializer kryoSerializer;
  private final ManagerDelegateGrpcClient managerDelegateGrpcClient;

  @Inject
  public ManagerDelegateServiceDriver(
      ManagerDelegateGrpcClient managerDelegateGrpcClient, KryoSerializer kryoSerializer) {
    this.managerDelegateGrpcClient = managerDelegateGrpcClient;
    this.kryoSerializer = kryoSerializer;
  }

  public <T extends ResponseData> T sendTask(
      String accountId, Map<String, String> setupAbstractions, TaskData taskData) {
    NgTaskDetails taskDetails = buildTaskDetails(taskData);
    SendTaskRequest request =
        SendTaskRequest.newBuilder()
            .setAccountId(NgAccountId.newBuilder().setId(accountId).build())
            .setSetupAbstractions(NgTaskSetupAbstractions.newBuilder().putAllValues(setupAbstractions).build())
            .setDetails(taskDetails)
            .build();
    final SendTaskResponse response = managerDelegateGrpcClient.sendTask(request, taskData.getTimeout());
    if (!response.getResponseData().isEmpty()) {
      return (T) kryoSerializer.asInflatedObject(response.getResponseData().toByteArray());
    }
    throw NoResultFoundException.newBuilder().message("no response found for sync delegate request").build();
  }

  public String sendTaskAsync(String accountId, Map<String, String> setupAbstractions, TaskData taskData) {
    SendTaskAsyncRequest request = buildSendTaskAsyncRequest(accountId, emptyIfNull(setupAbstractions), taskData);
    SendTaskAsyncResponse response = managerDelegateGrpcClient.sendTaskAsync(request);
    return response.getTaskId().getId();
  }

  public boolean abortTask(String accountId, String taskId) {
    try {
      AbortTaskRequest abortTaskRequest = AbortTaskRequest.newBuilder()
                                              .setTaskId(NgTaskId.newBuilder().setId(taskId).build())
                                              .setAccountId(NgAccountId.newBuilder().setId(accountId).build())
                                              .build();
      AbortTaskResponse response = managerDelegateGrpcClient.abortTask(abortTaskRequest);
      return response.getCanceledAtStage() != NgTaskExecutionStage.TYPE_UNSPECIFIED;
    } catch (RuntimeException ex) {
      logger.error("Failed to Abort Task: {}", ex.getMessage());
      return false;
    }
  }

  private SendTaskAsyncRequest buildSendTaskAsyncRequest(
      String accountId, Map<String, String> setupAbstractions, TaskData taskData) {
    NgTaskDetails taskDetails = buildTaskDetails(taskData);
    return SendTaskAsyncRequest.newBuilder()
        .setAccountId(NgAccountId.newBuilder().setId(accountId).build())
        .setSetupAbstractions(NgTaskSetupAbstractions.newBuilder().putAllValues(setupAbstractions).build())
        .setDetails(taskDetails)
        .build();
  }

  private NgTaskDetails buildTaskDetails(TaskData taskData) {
    return NgTaskDetails.newBuilder()
        .setType(NgTaskType.newBuilder().setType(taskData.getTaskType()).build())
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

  public String createRemotePerpetualTask(String perpetualTaskType, String accountId,
      RemotePerpetualTaskClientContext clientContext, RemotePerpetualTaskSchedule schedule, boolean allowDuplicate) {
    final CreateRemotePerpetualTaskRequest request = CreateRemotePerpetualTaskRequest.newBuilder()
                                                         .setAccountId(accountId)
                                                         .setAllowDuplicate(allowDuplicate)
                                                         .setTaskType(perpetualTaskType)
                                                         .setSchedule(schedule)
                                                         .setContext(clientContext)
                                                         .build();
    return managerDelegateGrpcClient.createRemotePerpetualTask(request).getPerpetualTaskId();
  }

  public boolean resetRemotePerpetualTask(String accountId, String taskId) {
    final ResetRemotePerpetualTaskRequest request =
        ResetRemotePerpetualTaskRequest.newBuilder().setAccountId(accountId).setPerpetualTaskId(taskId).build();
    return managerDelegateGrpcClient.resetRemotePerpetualTask(request).getSuccess();
  }

  public boolean deleteRemotePerpetualTask(String accountId, String taskId) {
    final DeleteRemotePerpetualTaskRequest request =
        DeleteRemotePerpetualTaskRequest.newBuilder().setAccountId(accountId).setPerpetualTaskId(taskId).build();
    return managerDelegateGrpcClient.deleteRemotePerpetualTask(request).getSuccess();
  }
}
