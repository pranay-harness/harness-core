package io.harness.service;

import io.harness.beans.DelegateTaskRequest;
import io.harness.callback.DelegateCallbackToken;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.grpc.DelegateServiceGrpcClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.function.Supplier;

@Singleton
public class DelegateGrpcClientWrapper {
  @Inject private DelegateServiceGrpcClient delegateServiceGrpcClient;
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  public DelegateResponseData executeSyncTask(DelegateTaskRequest delegateTaskRequest) {
    return delegateServiceGrpcClient.executeSyncTask(delegateTaskRequest, delegateCallbackTokenSupplier.get());
  }

  public String submitAsyncTask(DelegateTaskRequest delegateTaskRequest) {
    return delegateServiceGrpcClient.submitAsyncTask(delegateTaskRequest, delegateCallbackTokenSupplier.get());
  }
}
