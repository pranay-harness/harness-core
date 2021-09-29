package io.harness.steps.common;

import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.steps.executable.ChildExecutableWithRollbackAndRbac;
import io.harness.tasks.ResponseData;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class NGSectionStepWithRollbackInfo extends ChildExecutableWithRollbackAndRbac<NGSectionStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(OrchestrationStepTypes.NG_SECTION_WITH_ROLLBACK_INFO)
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  public String getGroupType() {
    return StepOutcomeGroup.EXECUTION.name();
  }

  @Override
  public void validateResources(Ambiance ambiance, NGSectionStepParameters stepParameters) {
    // Do Nothing
  }

  @Override
  public ChildExecutableResponse obtainChildAfterRbac(
      Ambiance ambiance, NGSectionStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Starting execution for " + stepParameters.getLogMessage() + " Step [{}]", stepParameters);
    return ChildExecutableResponse.newBuilder().setChildNodeId(stepParameters.getChildNodeId()).build();
  }

  @Override
  public StepResponse handleChildResponseInternal(
      Ambiance ambiance, NGSectionStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Completed execution for " + stepParameters.getLogMessage() + " Step [{}]", stepParameters);
    return createStepResponseFromChildResponse(responseDataMap);
  }

  @Override
  public Class<NGSectionStepParameters> getStepParametersClass() {
    return NGSectionStepParameters.class;
  }
}
