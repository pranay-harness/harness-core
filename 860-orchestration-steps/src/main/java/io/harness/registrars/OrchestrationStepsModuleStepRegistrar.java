package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.steps.approval.stage.ApprovalStageStep;
import io.harness.steps.approval.step.harness.HarnessApprovalStep;
import io.harness.steps.approval.step.jira.JiraApprovalStep;
import io.harness.steps.barriers.BarrierStep;
import io.harness.steps.common.pipeline.PipelineSetupStep;
import io.harness.steps.http.HttpStep;
import io.harness.steps.jira.create.JiraCreateStep;
import io.harness.steps.resourcerestraint.ResourceRestraintStep;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class OrchestrationStepsModuleStepRegistrar {
  public Map<StepType, Class<? extends Step>> getEngineSteps(boolean includeApprovalSteps) {
    Map<StepType, Class<? extends Step>> engineSteps = new HashMap<>();

    engineSteps.put(BarrierStep.STEP_TYPE, BarrierStep.class);
    engineSteps.put(ResourceRestraintStep.STEP_TYPE, ResourceRestraintStep.class);
    engineSteps.put(PipelineSetupStep.STEP_TYPE, PipelineSetupStep.class);

    if (includeApprovalSteps) {
      engineSteps.put(ApprovalStageStep.STEP_TYPE, ApprovalStageStep.class);
      engineSteps.put(HarnessApprovalStep.STEP_TYPE, HarnessApprovalStep.class);
      engineSteps.put(JiraApprovalStep.STEP_TYPE, JiraApprovalStep.class);
      engineSteps.put(JiraCreateStep.STEP_TYPE, JiraCreateStep.class);
    }

    engineSteps.put(HttpStep.STEP_TYPE, HttpStep.class);

    engineSteps.putAll(OrchestrationStepsModuleSdkStepRegistrar.getEngineSteps());

    return engineSteps;
  }
}
