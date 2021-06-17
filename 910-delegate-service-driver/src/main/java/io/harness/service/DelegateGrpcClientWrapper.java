package io.harness.service;

import io.harness.beans.DelegateTaskRequest;
import io.harness.callback.DelegateCallbackToken;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.function.Supplier;

@Singleton
public class DelegateGrpcClientWrapper {
  @Inject private DelegateServiceGrpcClient delegateServiceGrpcClient;
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;
  @Inject @Named("disableDeserialization") private boolean disableDeserialization;
  @Inject private KryoSerializer kryoSerializer;

  public DelegateResponseData executeSyncTask(DelegateTaskRequest delegateTaskRequest) {
    final ResponseData responseData = delegateServiceGrpcClient.executeSyncTaskReturningResponseData(
        delegateTaskRequest, delegateCallbackTokenSupplier.get());
    DelegateResponseData delegateResponseData;
    if (disableDeserialization) {
      delegateResponseData =
          (DelegateResponseData) kryoSerializer.asInflatedObject(((BinaryResponseData) responseData).getData());
    } else {
      delegateResponseData = (DelegateResponseData) responseData;
    }
    return delegateResponseData;
  }

  public String submitAsyncTask(DelegateTaskRequest delegateTaskRequest, Duration holdFor) {
    return delegateServiceGrpcClient.submitAsyncTask(delegateTaskRequest, delegateCallbackTokenSupplier.get(), holdFor);
  }
}
