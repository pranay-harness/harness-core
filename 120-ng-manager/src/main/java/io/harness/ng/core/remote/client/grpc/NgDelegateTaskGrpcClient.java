package io.harness.ng.core.remote.client.grpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.AbortTaskRequest;
import io.harness.delegate.NgDelegateTaskServiceGrpc.NgDelegateTaskServiceBlockingStub;

import java.util.concurrent.TimeUnit;

@Singleton
public class NgDelegateTaskGrpcClient {
  private final NgDelegateTaskServiceBlockingStub ngDelegateTaskServiceBlockingStub;

  @Inject
  public NgDelegateTaskGrpcClient(NgDelegateTaskServiceBlockingStub ngDelegateTaskServiceBlockingStub) {
    this.ngDelegateTaskServiceBlockingStub = ngDelegateTaskServiceBlockingStub;
  }

  public io.harness.delegate.SendTaskResponse sendTask(io.harness.delegate.SendTaskRequest request) {
    return ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).sendTask(request);
  }

  public io.harness.delegate.SendTaskAsyncResponse sendTaskAsync(io.harness.delegate.SendTaskAsyncRequest request) {
    return ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).sendTaskAsync(request);
  }

  public io.harness.delegate.AbortTaskResponse abortTask(io.harness.delegate.AbortTaskRequest request) {
    return ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS)
        .abortTask(AbortTaskRequest.newBuilder().build());
  }
}
