package io.harness.ngtriggers.utils;

import io.harness.beans.DelegateTaskRequest;
import io.harness.callback.DelegateCallbackToken;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.tasks.BinaryResponseData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class TaskExecutionUtils {
  private final DelegateServiceGrpcClient delegateServiceGrpcClient;
  private final Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  public BinaryResponseData executeSyncTask(DelegateTaskRequest taskRequest) {
    return (BinaryResponseData) delegateServiceGrpcClient.executeSyncTaskReturningResponseData(
        taskRequest, delegateCallbackTokenSupplier.get());
  }
}
