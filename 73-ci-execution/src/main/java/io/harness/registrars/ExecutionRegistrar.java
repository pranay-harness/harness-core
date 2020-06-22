package io.harness.registrars;

import io.harness.registries.registrar.StepRegistrar;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.states.BuildEnvSetupStep;
import io.harness.states.BuildStep;
import io.harness.states.CIPipelineSetupStep;
import io.harness.states.CleanupStep;
import io.harness.states.GitCloneStep;
import io.harness.states.IntegrationStageStep;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class ExecutionRegistrar implements StepRegistrar {
  @Override
  public void register(Set<Pair<StepType, Class<? extends Step>>> stateClasses) {
    stateClasses.add(Pair.of(BuildEnvSetupStep.STEP_TYPE, BuildEnvSetupStep.class));
    stateClasses.add(Pair.of(CleanupStep.STEP_TYPE, CleanupStep.class));
    stateClasses.add(Pair.of(BuildStep.STEP_TYPE, BuildStep.class));
    stateClasses.add(Pair.of(GitCloneStep.STEP_TYPE, GitCloneStep.class));
    stateClasses.add(Pair.of(IntegrationStageStep.STEP_TYPE, IntegrationStageStep.class));
    stateClasses.add(Pair.of(CIPipelineSetupStep.STEP_TYPE, CIPipelineSetupStep.class));
  }
}
