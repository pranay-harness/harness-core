package io.harness.cdng.orchestration;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.steps.InfrastructureSectionStep;
import io.harness.cdng.infra.steps.InfrastructureStep;
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
import io.harness.pms.sdk.registries.registrar.StepRegistrar;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public class NgStepRegistrar implements StepRegistrar {
  @Inject Injector injector;

  @Override
  public void register(Set<Pair<StepType, Step>> stateClasses) {
    // Add CDNG steps here
    stateClasses.add(
        Pair.of(RollbackOptionalChildChainStep.STEP_TYPE, injector.getInstance(RollbackOptionalChildChainStep.class)));
    stateClasses.add(
        Pair.of(RollbackOptionalChildrenStep.STEP_TYPE, injector.getInstance(RollbackOptionalChildrenStep.class)));
    stateClasses.add(Pair.of(NGSectionStep.STEP_TYPE, injector.getInstance(NGSectionStep.class)));
    stateClasses.add(Pair.of(PipelineSetupStep.STEP_TYPE, injector.getInstance(PipelineSetupStep.class)));
    stateClasses.add(
        Pair.of(InfrastructureSectionStep.STEP_TYPE, injector.getInstance(InfrastructureSectionStep.class)));
    stateClasses.add(Pair.of(InfrastructureStep.STEP_TYPE, injector.getInstance(InfrastructureStep.class)));
    stateClasses.add(Pair.of(DeploymentStageStep.STEP_TYPE, injector.getInstance(DeploymentStageStep.class)));
    stateClasses.add(Pair.of(ServiceStep.STEP_TYPE, injector.getInstance(ServiceStep.class)));
    stateClasses.add(Pair.of(K8sRollingStep.STEP_TYPE, injector.getInstance(K8sRollingStep.class)));
    stateClasses.add(Pair.of(K8sRollingRollbackStep.STEP_TYPE, injector.getInstance(K8sRollingRollbackStep.class)));
    stateClasses.add(Pair.of(HttpStep.STEP_TYPE, injector.getInstance(HttpStep.class)));
  }
}
