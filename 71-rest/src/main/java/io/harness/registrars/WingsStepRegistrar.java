package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.steps.InfrastructureSectionStep;
import io.harness.cdng.infra.steps.InfrastructureStep;
import io.harness.cdng.pipeline.steps.PipelineSetupStep;
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
    stateClasses.add(Pair.of(PipelineSetupStep.STEP_TYPE, PipelineSetupStep.class));
    stateClasses.add(Pair.of(InfrastructureSectionStep.STEP_TYPE, InfrastructureSectionStep.class));
    stateClasses.add(Pair.of(InfrastructureStep.STEP_TYPE, InfrastructureStep.class));
  }
}
