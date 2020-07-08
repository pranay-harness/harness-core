package io.harness.grpc;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.retry.Retry;
import io.harness.delegate.AbortTaskRequest;
import io.harness.delegate.AbortTaskResponse;
import io.harness.delegate.NgDelegateTaskServiceGrpc;
import io.harness.delegate.NgDelegateTaskServiceGrpc.NgDelegateTaskServiceBlockingStub;
import io.harness.delegate.SendTaskAsyncRequest;
import io.harness.delegate.SendTaskAsyncResponse;
import io.harness.delegate.SendTaskRequest;
import io.harness.delegate.SendTaskResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Singleton
@Slf4j
public class ManagerDelegateGrpcClient {
  private final NgDelegateTaskServiceBlockingStub ngDelegateTaskServiceBlockingStub;
  private final CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults(NgDelegateTaskServiceGrpc.SERVICE_NAME);
  private final Retry retry = Retry.ofDefaults(NgDelegateTaskServiceGrpc.SERVICE_NAME);
  private final Function<SendTaskRequest, SendTaskResponse> decoratedSendTask;
  private final Function<SendTaskAsyncRequest, SendTaskAsyncResponse> decoratedSendTaskAsync;
  private final Function<AbortTaskRequest, AbortTaskResponse> decoratedAbortTask;

  @Inject
  public ManagerDelegateGrpcClient(NgDelegateTaskServiceBlockingStub ngDelegateTaskServiceBlockingStub) {
    this.ngDelegateTaskServiceBlockingStub = ngDelegateTaskServiceBlockingStub;
    decoratedSendTask = Retry.decorateFunction(retry,
        CircuitBreaker.decorateFunction(circuitBreaker,
            r -> this.ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).sendTask(r)));
    decoratedSendTaskAsync = Retry.decorateFunction(retry,
        CircuitBreaker.decorateFunction(circuitBreaker,
            r -> this.ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).sendTaskAsync(r)));
    decoratedAbortTask = Retry.decorateFunction(retry,
        CircuitBreaker.decorateFunction(circuitBreaker,
            r -> this.ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).abortTask(r)));
  }

  public SendTaskResponse sendTask(SendTaskRequest request) {
    return decoratedSendTask.apply(request);
  }

  public SendTaskAsyncResponse sendTaskAsync(SendTaskAsyncRequest request) {
    return decoratedSendTaskAsync.apply(request);
  }

  public AbortTaskResponse abortTask(AbortTaskRequest request) {
    return decoratedAbortTask.apply(request);
  }

  @VisibleForTesting
  long getNumberOfFailedCallsWithRetryAttempt() {
    return this.retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt();
  }

  @VisibleForTesting
  long getNumberOfFailedCallsWithoutRetryAttempt() {
    return this.retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt();
  }

  @VisibleForTesting
  State getCircuitBreakerState() {
    return this.circuitBreaker.getState();
  }

  @VisibleForTesting
  long getNumberOfFailedCalls() {
    return this.circuitBreaker.getMetrics().getNumberOfFailedCalls();
  }
}
