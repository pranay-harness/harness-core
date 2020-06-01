package io.harness.engine.interrupts.steps;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.ambiance.Ambiance;
import io.harness.delay.SimpleNotifier;
import io.harness.delegate.beans.ResponseData;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.facilitator.modes.async.AsyncExecutableResponse.AsyncExecutableResponseBuilder;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.state.io.StepTransput;
import io.harness.waiter.StringNotifyResponseData;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SimpleAsyncStep implements Step, AsyncExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("SIMPLE_ASYNC").build();
  @Inject @Named("waitStateResumer") @Transient private ScheduledExecutorService executorService;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs) {
    SimpleStepAsyncParams params = (SimpleStepAsyncParams) stepParameters;
    String uuid = generateUuid();
    logger.info(
        "Executing ..." + SimpleAsyncStep.class.getName() + "..duration=" + params.getDuration() + ", uuid=" + uuid);
    AsyncExecutableResponseBuilder executionResponseBuilder = AsyncExecutableResponse.builder();
    executionResponseBuilder.callbackId(uuid);
    if (params.isShouldThrowException()) {
      throw new RuntimeException("Exception for test");
    }
    executorService.schedule(
        new SimpleNotifier(waitNotifyEngine, uuid, StringNotifyResponseData.builder().data("SUCCESS").build()),
        params.getDuration(), TimeUnit.SECONDS);
    return executionResponseBuilder.build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    StepResponseBuilder stepResponseBuilder = StepResponse.builder().status(Status.SUCCEEDED);
    for (Object response : responseDataMap.values()) {
      if (!"SUCCESS".equals(((StringNotifyResponseData) response).getData())) {
        stepResponseBuilder.status(Status.FAILED);
      }
    }
    return stepResponseBuilder.build();
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, StepParameters stateParameters, AsyncExecutableResponse executableResponse) {}
}
