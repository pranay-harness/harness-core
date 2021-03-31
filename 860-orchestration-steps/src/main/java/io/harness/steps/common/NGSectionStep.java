package io.harness.steps.common;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;
import static io.harness.steps.StepUtils.getFailedChildRollbackOutcome;

import io.harness.annotations.dev.OwnedBy;
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
@OwnedBy(PIPELINE)
public class NGSectionStep implements ChildExecutable<NGSectionStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(OrchestrationStepTypes.NG_SECTION).build();
  @Inject private OutcomeService outcomeService;

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, NGSectionStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Starting execution for " + stepParameters.getLogMessage() + " Step [{}]", stepParameters);
    return ChildExecutableResponse.newBuilder().setChildNodeId(stepParameters.getChildNodeId()).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, NGSectionStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Completed execution for " + stepParameters.getLogMessage() + " Step [{}]", stepParameters);
    StepResponse childResponse = createStepResponseFromChildResponse(responseDataMap);
    RollbackOutcome failedChildRollbackOutcome = getFailedChildRollbackOutcome(responseDataMap, outcomeService);
    if (failedChildRollbackOutcome != null) {
      String group;
      if (failedChildRollbackOutcome.getRollbackInfo().getGroup() == null) {
        group = RollbackNodeType.STAGE.name();
      } else if (failedChildRollbackOutcome.getRollbackInfo().getGroup().equals(RollbackNodeType.STEP.name())
          || failedChildRollbackOutcome.getRollbackInfo().getGroup().equals(RollbackNodeType.DIRECT_STAGE.name())) {
        group = RollbackNodeType.DIRECT_STAGE.name();
      } else {
        group = RollbackNodeType.STAGE.name();
      }
      return StepResponse.builder()
          .status(childResponse.getStatus())
          .failureInfo(childResponse.getFailureInfo())
          .stepOutcome(StepResponse.StepOutcome.builder()
                           .name("RollbackOutcome")
                           .outcome(RollbackOutcome.getClonedRollbackOutcome(
                               failedChildRollbackOutcome.getRollbackInfo(), stepParameters.getLogMessage(), group))
                           .build())
          .build();
    }
    return childResponse;
  }

  @Override
  public Class<NGSectionStepParameters> getStepParametersClass() {
    return NGSectionStepParameters.class;
  }
}
