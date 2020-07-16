package io.harness.grpc;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.retry.Retry;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.harness.delegate.AbortTaskRequest;
import io.harness.delegate.AbortTaskResponse;
import io.harness.delegate.NgDelegateTaskServiceGrpc;
import io.harness.delegate.NgDelegateTaskServiceGrpc.NgDelegateTaskServiceBlockingStub;
import io.harness.delegate.SendTaskAsyncRequest;
import io.harness.delegate.SendTaskAsyncResponse;
import io.harness.delegate.SendTaskRequest;
import io.harness.delegate.SendTaskResponse;
import io.harness.perpetualtask.CreateRemotePerpetualTaskRequest;
import io.harness.perpetualtask.CreateRemotePerpetualTaskResponse;
import io.harness.perpetualtask.DeleteRemotePerpetualTaskRequest;
import io.harness.perpetualtask.DeleteRemotePerpetualTaskResponse;
import io.harness.perpetualtask.ResetRemotePerpetualTaskRequest;
import io.harness.perpetualtask.ResetRemotePerpetualTaskResponse;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Singleton
@Slf4j
public class ManagerDelegateGrpcClient {
  private static final long SYNC_TASK_MAX_TIME_OUT_IN_SECONDS = 2 * 60;
  private static final long SYNC_TASK_MIN_TIME_OUT_IN_SECONDS = 5;
  private final NgDelegateTaskServiceBlockingStub ngDelegateTaskServiceBlockingStub;
  private final CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults(NgDelegateTaskServiceGrpc.SERVICE_NAME);
  private final Retry retry = Retry.ofDefaults(NgDelegateTaskServiceGrpc.SERVICE_NAME);
  private final Function<SendTaskRequestWithTimeOut, SendTaskResponse> decoratedSendTask;
  private final Function<SendTaskAsyncRequest, SendTaskAsyncResponse> decoratedSendTaskAsync;
  private final Function<AbortTaskRequest, AbortTaskResponse> decoratedAbortTask;
  private final Function<CreateRemotePerpetualTaskRequest, CreateRemotePerpetualTaskResponse>
      decoratedCreatePerpetualTask;
  private final Function<DeleteRemotePerpetualTaskRequest, DeleteRemotePerpetualTaskResponse>
      decoratedDeletePerpetualTask;
  private final Function<ResetRemotePerpetualTaskRequest, ResetRemotePerpetualTaskResponse> decoratedResetPerpetualTask;

  @Inject
  public ManagerDelegateGrpcClient(NgDelegateTaskServiceBlockingStub ngDelegateTaskServiceBlockingStub) {
    this.ngDelegateTaskServiceBlockingStub = ngDelegateTaskServiceBlockingStub;

    decoratedSendTask = decorateFunction(sendTaskFunction());
    decoratedSendTaskAsync = decorateFunction(sendTaskAsyncFunction());
    decoratedAbortTask = decorateFunction(abortTaskFunction());
    decoratedCreatePerpetualTask = decorateFunction(r
        -> ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).createRemotePerpetualTask(r),
        "create perpetual task");
    decoratedResetPerpetualTask = decorateFunction(r
        -> ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).resetRemotePerpetualTask(r),
        "reset perpetual task");
    decoratedDeletePerpetualTask = decorateFunction(r
        -> ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).deleteRemotePerpetualTask(r),
        "delete perpetual task");
  }

  public SendTaskResponse sendTask(SendTaskRequest request, long timeOutInSeconds) {
    return decoratedSendTask.apply(new SendTaskRequestWithTimeOut(request, timeOutInSeconds));
  }

  public SendTaskAsyncResponse sendTaskAsync(SendTaskAsyncRequest request) {
    return decoratedSendTaskAsync.apply(request);
  }

  public AbortTaskResponse abortTask(AbortTaskRequest request) {
    return decoratedAbortTask.apply(request);
  }

  private <T, R> Function<T, R> decorateWithExceptionLogger(Function<T, R> inputFunction, String actionName) {
    return t -> {
      R response = null;
      try {
        response = inputFunction.apply(t);
      } catch (StatusRuntimeException e) {
        logExceptionMessage(e, actionName);
      }
      return response;
    };
  }
  private Function<SendTaskRequestWithTimeOut, SendTaskResponse> sendTaskFunction() {
    return r -> {
      SendTaskResponse sendTaskResponse = null;
      try {
        sendTaskResponse = this.ngDelegateTaskServiceBlockingStub
                               .withDeadlineAfter(getMaxTimeOutInSeconds(r.getTimeoutInSeconds()), TimeUnit.SECONDS)
                               .sendTask(r.getSendTaskRequest());
      } catch (StatusRuntimeException e) {
        logExceptionMessage(e, "send task");
      }
      return sendTaskResponse;
    };
  }

  private Function<AbortTaskRequest, AbortTaskResponse> abortTaskFunction() {
    return r -> {
      AbortTaskResponse abortTaskResponse = null;
      try {
        abortTaskResponse = this.ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).abortTask(r);
      } catch (StatusRuntimeException e) {
        logExceptionMessage(e, "abort task");
      }
      return abortTaskResponse;
    };
  }

  private Function<SendTaskAsyncRequest, SendTaskAsyncResponse> sendTaskAsyncFunction() {
    return r -> {
      SendTaskAsyncResponse sendTaskAsyncResponse = null;
      try {
        sendTaskAsyncResponse =
            this.ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).sendTaskAsync(r);
      } catch (StatusRuntimeException e) {
        logExceptionMessage(e, "sent task async");
      }
      return sendTaskAsyncResponse;
    };
  }

  public CreateRemotePerpetualTaskResponse createRemotePerpetualTask(CreateRemotePerpetualTaskRequest request) {
    return decoratedCreatePerpetualTask.apply(request);
  }

  public DeleteRemotePerpetualTaskResponse deleteRemotePerpetualTask(DeleteRemotePerpetualTaskRequest request) {
    return decoratedDeletePerpetualTask.apply(request);
  }

  public ResetRemotePerpetualTaskResponse resetRemotePerpetualTask(ResetRemotePerpetualTaskRequest request) {
    return decoratedResetPerpetualTask.apply(request);
  }

  private <T, R> Function<T, R> decorateFunction(Function<T, R> inputFunction) {
    return Retry.decorateFunction(retry, CircuitBreaker.decorateFunction(circuitBreaker, inputFunction));
  }
  private <T, R> Function<T, R> decorateFunction(Function<T, R> inputFunction, String actionName) {
    return Retry.decorateFunction(
        retry, CircuitBreaker.decorateFunction(circuitBreaker, decorateWithExceptionLogger(inputFunction, actionName)));
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

  private void logExceptionMessage(StatusRuntimeException exception, String action) {
    if (exception.getStatus().getCode() == Status.Code.INTERNAL) {
      logger.error("Exception occurred during {} execution", action, exception);
    } else if (exception.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
      logger.error("{} action was timed out", action, exception);
    } else if (exception.getStatus().getCode() == Status.Code.UNAUTHENTICATED) {
      logger.error("Authentication failed when trying to execute {}", action, exception);
      throw exception;
    } else if (exception.getStatus().getCode() == Status.Code.UNAVAILABLE) {
      logger.error("Delegate service is unavailable when trying to {}", action, exception);
      throw exception;
    } else {
      logger.error("Exception occurring while trying to {}", action, exception);
    }
  }

  private long getMaxTimeOutInSeconds(long taskTimeoutInSeconds) {
    if (taskTimeoutInSeconds == 0) {
      return SYNC_TASK_MIN_TIME_OUT_IN_SECONDS;
    }
    return Math.min(taskTimeoutInSeconds, SYNC_TASK_MAX_TIME_OUT_IN_SECONDS);
  }

  @Value
  @AllArgsConstructor
  private static class SendTaskRequestWithTimeOut {
    private SendTaskRequest sendTaskRequest;
    private long timeoutInSeconds;
  }
}
