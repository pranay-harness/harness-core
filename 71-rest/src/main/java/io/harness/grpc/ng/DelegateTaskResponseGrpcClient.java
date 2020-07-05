package io.harness.grpc.ng;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.NgDelegateTaskResponseServiceGrpc;

import java.util.concurrent.TimeUnit;

@Singleton
public class DelegateTaskResponseGrpcClient {
  private final NgDelegateTaskResponseServiceGrpc
      .NgDelegateTaskResponseServiceBlockingStub ngDelegateTaskResponseServiceBlockingStub;

  @Inject
  public DelegateTaskResponseGrpcClient(NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceBlockingStub
                                            ngDelegateTaskResponseServiceBlockingStub) {
    this.ngDelegateTaskResponseServiceBlockingStub = ngDelegateTaskResponseServiceBlockingStub;
  }

  public io.harness.delegate.SendTaskResultResponse sendTaskResult(io.harness.delegate.SendTaskResultRequest request) {
    return ngDelegateTaskResponseServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).sendTaskResult(request);
  }
}
