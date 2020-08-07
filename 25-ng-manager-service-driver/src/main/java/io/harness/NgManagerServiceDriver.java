package io.harness;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.harness.delegate.NgDelegateTaskResponseServiceGrpc;
import io.harness.delegate.NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceBlockingStub;
import io.harness.delegate.SendTaskResultRequest;
import io.harness.delegate.SendTaskResultResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsRequest;
import io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse;
import io.harness.perpetualtask.ObtainPerpetualTaskValidationDetailsRequest;
import io.harness.perpetualtask.ObtainPerpetualTaskValidationDetailsResponse;
import io.harness.serializer.KryoSerializer;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Singleton
@Slf4j
public class NgManagerServiceDriver {
  private final NgDelegateTaskResponseServiceBlockingStub ngDelegateTaskServiceBlockingStub;
  private final KryoSerializer kryoSerializer;
  private final Function<SendTaskResultRequest, SendTaskResultResponse> decoratedSendTaskResultFunction;
  private final Function<ObtainPerpetualTaskExecutionParamsRequest, ObtainPerpetualTaskExecutionParamsResponse>
      decoratedObtainPerpetualTaskParamsFunction;
  private final Function<ObtainPerpetualTaskValidationDetailsRequest, ObtainPerpetualTaskValidationDetailsResponse>
      decoratedObtainPerpetualTaskValidationDetailFunction;
  private final Retry retry;

  @Inject
  public NgManagerServiceDriver(
      NgDelegateTaskResponseServiceBlockingStub ngDelegateTaskServiceBlockingStub, KryoSerializer kryoSerializer) {
    this.ngDelegateTaskServiceBlockingStub = ngDelegateTaskServiceBlockingStub;
    this.kryoSerializer = kryoSerializer;
    retry = Retry.of(NgDelegateTaskResponseServiceGrpc.SERVICE_NAME, this ::getRetryConfig);
    decoratedSendTaskResultFunction = decorateFunction(sendTaskResultFunction());
    decoratedObtainPerpetualTaskValidationDetailFunction = decorateFunction(request
        -> ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS)
               .obtainPerpetualTaskValidationDetails(request));

    decoratedObtainPerpetualTaskParamsFunction = decorateFunction(request
        -> ngDelegateTaskServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS)
               .obtainPerpetualTaskExecutionParams(request));
  }

  public boolean sendTaskResult(String taskId, ResponseData responseData) {
    SendTaskResultRequest resultRequest =
        SendTaskResultRequest.newBuilder()
            .setTaskId(taskId)
            .setResponseData(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(responseData)))
            .build();
    SendTaskResultResponse response = decoratedSendTaskResultFunction.apply(resultRequest);
    return response.getAcknowledgement();
  }

  private Function<SendTaskResultRequest, SendTaskResultResponse> sendTaskResultFunction() {
    return r -> {
      SendTaskResultResponse sendTaskResultResponse = null;
      try {
        sendTaskResultResponse =
            ngDelegateTaskServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS).sendTaskResult(r);
      } catch (StatusRuntimeException sre) {
        logExceptionMessage(sre);
      }
      return sendTaskResultResponse;
    };
  }

  private <T, R> Function<T, R> decorateFunction(Function<T, R> inputFunction) {
    return Retry.decorateFunction(retry, t -> {
      R response = null;
      try {
        response = inputFunction.apply(t);
      } catch (StatusRuntimeException sre) {
        logExceptionMessage(sre);
      }
      return response;
    });
  }

  private RetryConfig getRetryConfig() {
    return RetryConfig.custom()
        .maxAttempts(3)
        .waitDuration(Duration.ofSeconds(10))
        .intervalFunction(IntervalFunction.of(Duration.ofSeconds(15)))
        .retryOnException(
            e -> e instanceof StatusRuntimeException && ((StatusRuntimeException) e).getStatus() == Status.UNAVAILABLE)
        .build();
  }

  private void logExceptionMessage(StatusRuntimeException exception) {
    if (exception.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
      logger.error("sendTaskResultFunction action was timed out", exception);
    } else if (exception.getStatus().getCode() == Status.Code.UNAUTHENTICATED) {
      logger.error(
          "Authentication failed on target service when trying to execute sendTaskResultFunction action", exception);
      throw exception;
    } else if (exception.getStatus().getCode() == Status.Code.UNAVAILABLE) {
      logger.error("Target service is unavailable when trying to sendTaskResultFunction", exception);
      throw exception;
    } else {
      logger.error("Exception occurring while trying to sendTaskResultFunction", exception);
    }
  }

  @VisibleForTesting
  long getNumberOfFailedCallsWithRetryAttempt() {
    return this.retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt();
  }

  @VisibleForTesting
  long getNumberOfFailedCallsWithoutRetryAttempt() {
    return this.retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt();
  }

  public ObtainPerpetualTaskValidationDetailsResponse obtainPerpetualTaskValidationDetails(
      ObtainPerpetualTaskValidationDetailsRequest request) {
    return decoratedObtainPerpetualTaskValidationDetailFunction.apply(request);
  }

  public ObtainPerpetualTaskExecutionParamsResponse obtainPerpetualTaskExecutionParams(
      ObtainPerpetualTaskExecutionParamsRequest request) {
    return decoratedObtainPerpetualTaskParamsFunction.apply(request);
  }
}
