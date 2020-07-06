package io.harness;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;

import io.harness.delegate.AbortTaskRequest;
import io.harness.delegate.AccountId;
import io.harness.delegate.NgDelegateTaskServiceGrpc.NgDelegateTaskServiceBlockingStub;
import io.harness.delegate.SendTaskRequest;
import io.harness.delegate.SendTaskResponse;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.delegate.TaskType;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.serializer.KryoSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class ManagerDelegateServiceDriver {
  private final NgDelegateTaskServiceBlockingStub ngDelegateTaskServiceBlockingStub;
  private final KryoSerializer kryoSerializer;

  @Inject
  public ManagerDelegateServiceDriver(
      NgDelegateTaskServiceBlockingStub ngDelegateTaskServiceBlockingStub, KryoSerializer kryoSerializer) {
    this.ngDelegateTaskServiceBlockingStub = ngDelegateTaskServiceBlockingStub;
    this.kryoSerializer = kryoSerializer;
  }

  public SendTaskResponse sendTask(String accountId, Map<String, String> setupAbstractions, TaskData taskData) {
    TaskDetails taskDetails =
        TaskDetails.newBuilder()
            .setType(TaskType.newBuilder().setType(taskData.getTaskType()).build())
            .setKryoParameters(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(getTaskParameter(taskData))))
            .putAllExpressions(taskData.getExpressions() == null ? new HashMap<>() : taskData.getExpressions())
            .setExpressionFunctorToken(taskData.getExpressionFunctorToken())
            .setExecutionTimeout(Duration.newBuilder().setSeconds(taskData.getTimeout() * 1000).build())
            .build();

    SendTaskRequest request =
        SendTaskRequest.newBuilder()
            .setAccountId(AccountId.newBuilder().setId(accountId).build())
            .setSetupAbstractions(TaskSetupAbstractions.newBuilder().putAllValues(setupAbstractions).build())
            .setDetails(taskDetails)
            .build();
    return ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).sendTask(request);
  }

  public io.harness.delegate.SendTaskAsyncResponse sendTaskAsync(io.harness.delegate.SendTaskAsyncRequest request) {
    return ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).sendTaskAsync(request);
  }

  public io.harness.delegate.AbortTaskResponse abortTask(io.harness.delegate.AbortTaskRequest request) {
    return ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS)
        .abortTask(AbortTaskRequest.newBuilder().build());
  }

  private TaskParameters getTaskParameter(TaskData taskData) {
    Object[] parameters = taskData.getParameters();
    if (parameters.length == 1 && parameters[0] instanceof TaskParameters) {
      return (TaskParameters) parameters[0];
    }
    throw new InvalidRequestException("Only Supported for task using task parameters");
  }
}
