package io.harness.registrars;

import io.harness.beans.steps.BuildEnvSetupStepInfo;
import io.harness.beans.steps.BuildStepInfo;
import io.harness.beans.steps.CleanupStepInfo;
import io.harness.registries.registrar.StateRegistrar;
import io.harness.state.State;
import io.harness.state.StateType;
import io.harness.states.BuildEnvSetupState;
import io.harness.states.BuildState;
import io.harness.states.CleanupState;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class ExecutionRegistrar implements StateRegistrar {
  @Override
  public void register(Set<Pair<StateType, Class<? extends State>>> stateClasses) {
    stateClasses.add(Pair.of(BuildEnvSetupStepInfo.stateType, BuildEnvSetupState.class));
    stateClasses.add(Pair.of(CleanupStepInfo.stateType, CleanupState.class));
    stateClasses.add(Pair.of(BuildStepInfo.stateType, BuildState.class));
  }
}
