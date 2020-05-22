package io.harness.registrars;

import io.harness.beans.steps.BuildEnvSetupStepInfo;
import io.harness.beans.steps.BuildStepInfo;
import io.harness.beans.steps.CleanupStepInfo;
import io.harness.registries.registrar.StepRegistrar;
import io.harness.state.StateType;
import io.harness.state.Step;
import io.harness.states.BuildEnvSetupStep;
import io.harness.states.BuildStep;
import io.harness.states.CleanupStep;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class ExecutionRegistrar implements StepRegistrar {
  @Override
  public void register(Set<Pair<StateType, Class<? extends Step>>> stateClasses) {
    stateClasses.add(Pair.of(BuildEnvSetupStepInfo.stateType, BuildEnvSetupStep.class));
    stateClasses.add(Pair.of(CleanupStepInfo.stateType, CleanupStep.class));
    stateClasses.add(Pair.of(BuildStepInfo.stateType, BuildStep.class));
  }
}
