package io.harness.cdng.orchestration;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.steps.InfrastructureSectionStep;
import io.harness.cdng.infra.steps.InfrastructureStep;
import io.harness.cdng.k8s.K8sBlueGreenStep;
import io.harness.cdng.k8s.K8sRollingRollbackStep;
import io.harness.cdng.k8s.K8sRollingStep;
import io.harness.cdng.pipeline.steps.DeploymentStageStep;
import io.harness.cdng.pipeline.steps.HttpStep;
import io.harness.cdng.pipeline.steps.NGSectionStep;
import io.harness.cdng.pipeline.steps.PipelineSetupStep;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildChainStep;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildrenStep;
import io.harness.cdng.service.steps.ServiceStep;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.registrars.OrchestrationStepsModuleStepRegistrar;

import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class NgStepRegistrar {
  public Map<StepType, Step> getEngineSteps(Injector injector) {
    Map<StepType, Step> engineSteps = new HashMap<>();

    // Add CDNG steps here
    engineSteps.put(
        RollbackOptionalChildChainStep.STEP_TYPE, injector.getInstance(RollbackOptionalChildChainStep.class));
    engineSteps.put(RollbackOptionalChildrenStep.STEP_TYPE, injector.getInstance(RollbackOptionalChildrenStep.class));
    engineSteps.put(NGSectionStep.STEP_TYPE, injector.getInstance(NGSectionStep.class));
    engineSteps.put(PipelineSetupStep.STEP_TYPE, injector.getInstance(PipelineSetupStep.class));
    engineSteps.put(InfrastructureSectionStep.STEP_TYPE, injector.getInstance(InfrastructureSectionStep.class));
    engineSteps.put(InfrastructureStep.STEP_TYPE, injector.getInstance(InfrastructureStep.class));
    engineSteps.put(DeploymentStageStep.STEP_TYPE, injector.getInstance(DeploymentStageStep.class));
    engineSteps.put(ServiceStep.STEP_TYPE, injector.getInstance(ServiceStep.class));
    engineSteps.put(K8sRollingStep.STEP_TYPE, injector.getInstance(K8sRollingStep.class));
    engineSteps.put(K8sRollingRollbackStep.STEP_TYPE, injector.getInstance(K8sRollingRollbackStep.class));
    engineSteps.put(HttpStep.STEP_TYPE, injector.getInstance(HttpStep.class));
    engineSteps.put(K8sBlueGreenStep.STEP_TYPE, injector.getInstance(K8sBlueGreenStep.class));

    engineSteps.putAll(OrchestrationStepsModuleStepRegistrar.getEngineSteps(injector));
    return engineSteps;
  }
}
