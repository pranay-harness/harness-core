package io.harness.steps.common.steps.stepgroup;

import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;
import static io.harness.steps.StepUtils.getFailedChildRollbackOutcome;

import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.adviser.rollback.RollbackNodeType;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.RollbackOutcome;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StepGroupStep implements ChildExecutable<StepGroupStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(OrchestrationStepTypes.STEP_GROUP).build();
  @Inject private OutcomeService outcomeService;

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, StepGroupStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Starting StepGroup for Pipeline Step [{}]", stepParameters);
    final String stepNodeId = stepParameters.getChildNodeID();
    return ChildExecutableResponse.newBuilder().setChildNodeId(stepNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, StepGroupStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Executed StepGroup Step =[{}]", stepParameters);
    StepResponse childResponse = createStepResponseFromChildResponse(responseDataMap);
    RollbackOutcome failedChildRollbackOutcome = getFailedChildRollbackOutcome(responseDataMap, outcomeService);
    if (failedChildRollbackOutcome != null) {
      return StepResponse.builder()
          .status(childResponse.getStatus())
          .failureInfo(childResponse.getFailureInfo())
          .stepOutcome(
              StepResponse.StepOutcome.builder()
                  .name("RollbackOutcome")
                  .outcome(RollbackOutcome.getClonedRollbackOutcome(failedChildRollbackOutcome.getRollbackInfo(),
                      stepParameters.getIdentifier(), RollbackNodeType.STEP_GROUP.name()))
                  .build())
          .build();
    }
    return childResponse;
  }

  @Override
  public Class<StepGroupStepParameters> getStepParametersClass() {
    return StepGroupStepParameters.class;
  }
}
