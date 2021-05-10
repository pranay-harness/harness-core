package io.harness.cdng.orchestration;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.steps.InfrastructureSectionStep;
import io.harness.cdng.infra.steps.InfrastructureStep;
import io.harness.cdng.k8s.K8sApplyStep;
import io.harness.cdng.k8s.K8sBGSwapServicesStep;
import io.harness.cdng.k8s.K8sBlueGreenStep;
import io.harness.cdng.k8s.K8sCanaryDeleteStep;
import io.harness.cdng.k8s.K8sCanaryStep;
import io.harness.cdng.k8s.K8sDeleteStep;
import io.harness.cdng.k8s.K8sRollingRollbackStep;
import io.harness.cdng.k8s.K8sRollingStep;
import io.harness.cdng.k8s.K8sScaleStep;
import io.harness.cdng.pipeline.steps.DeploymentStageStep;
import io.harness.cdng.pipeline.steps.NGSectionStep;
import io.harness.cdng.pipeline.steps.PipelineSetupStep;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildChainStep;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildrenStep;
import io.harness.cdng.provision.terraform.TerraformApplyStep;
import io.harness.cdng.provision.terraform.TerraformDestroyStep;
import io.harness.cdng.provision.terraform.TerraformPlanStep;
import io.harness.cdng.provision.terraform.steps.rolllback.TerraformRollbackStep;
import io.harness.cdng.service.steps.ServiceStep;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.registrars.OrchestrationStepsModuleSdkStepRegistrar;
import io.harness.steps.common.script.ShellScriptStep;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class NgStepRegistrar {
  public Map<StepType, Class<? extends Step>> getEngineSteps() {
    Map<StepType, Class<? extends Step>> engineSteps = new HashMap<>();

    // Add CDNG steps here
    engineSteps.put(RollbackOptionalChildChainStep.STEP_TYPE, RollbackOptionalChildChainStep.class);
    engineSteps.put(RollbackOptionalChildrenStep.STEP_TYPE, RollbackOptionalChildrenStep.class);
    engineSteps.put(NGSectionStep.STEP_TYPE, NGSectionStep.class);
    engineSteps.put(PipelineSetupStep.STEP_TYPE, PipelineSetupStep.class);
    engineSteps.put(InfrastructureSectionStep.STEP_TYPE, InfrastructureSectionStep.class);
    engineSteps.put(InfrastructureStep.STEP_TYPE, InfrastructureStep.class);
    engineSteps.put(DeploymentStageStep.STEP_TYPE, DeploymentStageStep.class);
    engineSteps.put(ServiceStep.STEP_TYPE, ServiceStep.class);
    engineSteps.put(K8sDeleteStep.STEP_TYPE, K8sDeleteStep.class);
    engineSteps.put(K8sRollingStep.STEP_TYPE, K8sRollingStep.class);
    engineSteps.put(K8sRollingRollbackStep.STEP_TYPE, K8sRollingRollbackStep.class);
    engineSteps.put(K8sScaleStep.STEP_TYPE, K8sScaleStep.class);
    engineSteps.put(K8sCanaryStep.STEP_TYPE, K8sCanaryStep.class);
    engineSteps.put(K8sCanaryDeleteStep.STEP_TYPE, K8sCanaryDeleteStep.class);
    engineSteps.put(K8sBlueGreenStep.STEP_TYPE, K8sBlueGreenStep.class);
    engineSteps.put(K8sBGSwapServicesStep.STEP_TYPE, K8sBGSwapServicesStep.class);
    engineSteps.put(ShellScriptStep.STEP_TYPE, ShellScriptStep.class);
    engineSteps.put(K8sApplyStep.STEP_TYPE, K8sApplyStep.class);
    engineSteps.put(TerraformApplyStep.STEP_TYPE, TerraformApplyStep.class);
    engineSteps.put(TerraformPlanStep.STEP_TYPE, TerraformPlanStep.class);
    engineSteps.put(TerraformDestroyStep.STEP_TYPE, TerraformDestroyStep.class);
    engineSteps.put(TerraformRollbackStep.STEP_TYPE, TerraformRollbackStep.class);

    engineSteps.putAll(OrchestrationStepsModuleSdkStepRegistrar.getEngineSteps());
    return engineSteps;
  }
}
