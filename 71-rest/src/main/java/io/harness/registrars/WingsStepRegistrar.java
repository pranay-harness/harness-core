package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.steps.ArtifactForkStep;
import io.harness.cdng.artifact.steps.ArtifactStep;
import io.harness.cdng.environment.steps.EnvironmentStep;
import io.harness.cdng.infra.steps.InfrastructureSectionStep;
import io.harness.cdng.infra.steps.InfrastructureStep;
import io.harness.cdng.k8s.K8sRollingRollbackStep;
import io.harness.cdng.k8s.K8sRollingStep;
import io.harness.cdng.manifest.state.ManifestStep;
import io.harness.cdng.pipeline.steps.DeploymentStageStep;
import io.harness.cdng.pipeline.steps.PipelineSetupStep;
import io.harness.cdng.service.steps.ServiceStep;
import io.harness.cdng.tasks.manifestFetch.step.ManifestFetchStep;
import io.harness.redesign.states.email.EmailStep;
import io.harness.redesign.states.http.BasicHttpStep;
import io.harness.redesign.states.http.chain.BasicHttpChainStep;
import io.harness.redesign.states.shell.ShellScriptStep;
import io.harness.redesign.states.wait.WaitStep;
import io.harness.registries.registrar.StepRegistrar;
import io.harness.state.Step;
import io.harness.state.StepType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public class WingsStepRegistrar implements StepRegistrar {
  @Override
  public void register(Set<Pair<StepType, Class<? extends Step>>> stateClasses) {
    // Add CORE/Example/Experimental States Here
    stateClasses.add(Pair.of(BasicHttpStep.STEP_TYPE, BasicHttpStep.class));
    stateClasses.add(Pair.of(WaitStep.STEP_TYPE, WaitStep.class));
    stateClasses.add(Pair.of(ShellScriptStep.STEP_TYPE, ShellScriptStep.class));
    stateClasses.add(Pair.of(EmailStep.STEP_TYPE, EmailStep.class));
    stateClasses.add(Pair.of(BasicHttpChainStep.STEP_TYPE, BasicHttpChainStep.class));

    // Add CDNG steps here
    stateClasses.add(Pair.of(ArtifactStep.STEP_TYPE, ArtifactStep.class));
    stateClasses.add(Pair.of(ArtifactForkStep.STEP_TYPE, ArtifactForkStep.class));
    stateClasses.add(Pair.of(ManifestStep.STEP_TYPE, ManifestStep.class));
    stateClasses.add(Pair.of(ManifestFetchStep.STEP_TYPE, ManifestFetchStep.class));
    stateClasses.add(Pair.of(PipelineSetupStep.STEP_TYPE, PipelineSetupStep.class));
    stateClasses.add(Pair.of(InfrastructureSectionStep.STEP_TYPE, InfrastructureSectionStep.class));
    stateClasses.add(Pair.of(EnvironmentStep.STEP_TYPE, EnvironmentStep.class));
    stateClasses.add(Pair.of(InfrastructureStep.STEP_TYPE, InfrastructureStep.class));
    stateClasses.add(Pair.of(DeploymentStageStep.STEP_TYPE, DeploymentStageStep.class));
    stateClasses.add(Pair.of(ServiceStep.STEP_TYPE, ServiceStep.class));
    stateClasses.add(Pair.of(K8sRollingStep.STEP_TYPE, K8sRollingStep.class));
    stateClasses.add(Pair.of(K8sRollingRollbackStep.STEP_TYPE, K8sRollingRollbackStep.class));
  }
}
