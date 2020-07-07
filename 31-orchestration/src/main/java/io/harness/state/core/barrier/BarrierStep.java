package io.harness.state.core.barrier;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.distribution.barrier.Barrier.State.DOWN;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.barriers.BarrierExecutionInstance;
import io.harness.barriers.BarrierResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.barriers.BarrierService;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.status.Status;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.persistence.HPersistence;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@OwnedBy(CDC)
@Slf4j
public class BarrierStep implements Step, SyncExecutable, AsyncExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("BARRIER").build();

  @Inject private BarrierService barrierService;
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public StepResponse executeSync(Ambiance ambiance, StepParameters stepParameters, StepInputPackage inputPackage,
      PassThroughData passThroughData) {
    final String identifier = ((BarrierStepParameters) stepParameters).getIdentifier();
    logger.warn("There is only one barrier present for planExecution [{}] with [{}] identifier, passing through it...",
        ambiance.getPlanExecutionId(), identifier);
    BarrierExecutionInstance barrierExecutionInstance = barrierService.get(ambiance.obtainCurrentRuntimeId());
    barrierExecutionInstance.setBarrierState(DOWN);
    HPersistence.retry(() -> barrierService.save(barrierExecutionInstance));
    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name("barrier")
                         .outcome(BarrierOutcome.builder()
                                      .message("There is only one barrier present. Barrier went down")
                                      .identifier(identifier)
                                      .build())
                         .build())
        .build();
  }

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, StepParameters stepParameters, StepInputPackage inputPackage) {
    BarrierExecutionInstance barrierExecutionInstance = barrierService.get(ambiance.obtainCurrentRuntimeId());
    NodeExecution nodeExecution = nodeExecutionService.get(barrierExecutionInstance.getUuid());
    nodeExecution.setExpiryTs(barrierExecutionInstance.getExpiredIn() + System.currentTimeMillis());

    nodeExecutionService.save(nodeExecution);
    barrierService.update(barrierExecutionInstance);

    return AsyncExecutableResponse.builder().callbackId(barrierExecutionInstance.getIdentifier()).build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    BarrierExecutionInstance barrierExecutionInstance =
        updateBarrierExecutionInstance(ambiance.obtainCurrentRuntimeId());

    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    BarrierResponseData responseData =
        (BarrierResponseData) responseDataMap.get(barrierExecutionInstance.getIdentifier());
    if (responseData.isFailed()) {
      stepResponseBuilder.status(Status.FAILED)
          .failureInfo(FailureInfo.builder().errorMessage(responseData.getErrorMessage()).build());
    } else {
      stepResponseBuilder.status(Status.SUCCEEDED);
    }

    return stepResponseBuilder
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name("barrier")
                         .outcome(BarrierOutcome.builder().identifier(barrierExecutionInstance.getIdentifier()).build())
                         .build())
        .build();
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, StepParameters stateParameters, AsyncExecutableResponse executableResponse) {
    updateBarrierExecutionInstance(ambiance.obtainCurrentRuntimeId());
  }

  private BarrierExecutionInstance updateBarrierExecutionInstance(String barrierExecutionId) {
    BarrierExecutionInstance barrierExecutionInstance = barrierService.get(barrierExecutionId);
    return barrierService.update(barrierExecutionInstance);
  }
}
