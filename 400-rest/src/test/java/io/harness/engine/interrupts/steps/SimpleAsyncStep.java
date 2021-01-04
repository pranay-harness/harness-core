package io.harness.engine.interrupts.steps;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.delay.SimpleNotifier;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.tasks.ResponseData;
import io.harness.waiter.StringNotifyResponseData;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;

@Slf4j
public class SimpleAsyncStep implements AsyncExecutable<SimpleStepAsyncParams> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType("SIMPLE_ASYNC").build();
  @Inject @Named("waitStateResumer") @Transient private ScheduledExecutorService executorService;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public Class<SimpleStepAsyncParams> getStepParametersClass() {
    return SimpleStepAsyncParams.class;
  }

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, SimpleStepAsyncParams simpleStepAsyncParams, StepInputPackage inputPackage) {
    String uuid = generateUuid();
    log.info("Executing ..." + SimpleAsyncStep.class.getName() + "..duration=" + simpleStepAsyncParams.getDuration()
        + ", uuid=" + uuid);
    AsyncExecutableResponse.Builder builder = AsyncExecutableResponse.newBuilder();
    builder.addCallbackIds(uuid);
    if (simpleStepAsyncParams.isShouldThrowException()) {
      throw new RuntimeException("Exception for test");
    }

    StringNotifyResponseData stringNotifyResponseData =
        StringNotifyResponseData.builder().data(simpleStepAsyncParams.isShouldFail() ? "FAIL" : "SUCCESS").build();
    executorService.schedule(new SimpleNotifier(waitNotifyEngine, uuid, stringNotifyResponseData),
        simpleStepAsyncParams.getDuration(), TimeUnit.SECONDS);
    return builder.build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, SimpleStepAsyncParams stepParameters, Map<String, ResponseData> responseDataMap) {
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
      Ambiance ambiance, SimpleStepAsyncParams stateParameters, AsyncExecutableResponse executableResponse) {}
}
