package io.harness.plancreator.steps.internal;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.plancreator.steps.barrier.BarrierStepInfo;
import io.harness.plancreator.steps.common.StepElementParameters.StepElementParametersBuilder;
import io.harness.plancreator.steps.common.StepParametersUtils;
import io.harness.plancreator.steps.common.StepSpecParameters;
import io.harness.plancreator.steps.common.WithRollbackInfo;
import io.harness.plancreator.steps.http.HttpStepInfo;
import io.harness.pms.sdk.core.steps.io.BaseStepParameterInfo;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.steps.approval.step.harness.HarnessApprovalStepInfo;
import io.harness.steps.approval.step.jira.JiraApprovalStepInfo;
import io.harness.steps.jira.create.JiraCreateStepInfo;
import io.harness.steps.jira.update.JiraUpdateStepInfo;
import io.harness.yaml.core.StepSpecType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;

@OwnedBy(PIPELINE)
@ApiModel(subTypes = {BarrierStepInfo.class, HttpStepInfo.class, HarnessApprovalStepInfo.class,
              JiraApprovalStepInfo.class, JiraCreateStepInfo.class, JiraUpdateStepInfo.class})
public interface PMSStepInfo extends StepSpecType, WithRollbackInfo {
  @Override
  default StepParameters getStepParametersWithRollbackInfo(BaseStepParameterInfo baseStepParameterInfo) {
    return null;
  }

  default StepParameters getStepParametersInfo(StepElementConfig stepElementConfig) {
    StepElementParametersBuilder stepParametersBuilder = StepParametersUtils.getStepParameters(stepElementConfig);
    stepParametersBuilder.spec(getStepSpecParameters());
    return stepParametersBuilder.build();
  }

  @JsonIgnore
  default StepSpecParameters getStepSpecParameters() {
    return null;
  }

  @Override
  default boolean validateStageFailureStrategy() {
    return false;
  }
}
