package io.harness;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceBlockingStub;
import io.harness.delegate.SendTaskResultRequest;
import io.harness.delegate.SendTaskResultResponse;
import io.harness.serializer.KryoSerializer;

import java.util.concurrent.TimeUnit;

@Singleton
public class NgManagerServiceDriver {
  private final NgDelegateTaskResponseServiceBlockingStub ngDelegateTaskServiceBlockingStub;
  private final KryoSerializer kryoSerializer;

  @Inject
  public NgManagerServiceDriver(
      NgDelegateTaskResponseServiceBlockingStub ngDelegateTaskServiceBlockingStub, KryoSerializer kryoSerializer) {
    this.ngDelegateTaskServiceBlockingStub = ngDelegateTaskServiceBlockingStub;
    this.kryoSerializer = kryoSerializer;
  }

  public SendTaskResultResponse sendTaskResult(SendTaskResultRequest request) {
    return ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).sendTaskResult(request);
  }
}
