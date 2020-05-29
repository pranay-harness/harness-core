package io.harness.registrars;

import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.steps.stepinfo.BuildStepInfo;
import io.harness.beans.steps.stepinfo.CleanupStepInfo;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.registries.registrar.StepRegistrar;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.states.BuildEnvSetupStep;
import io.harness.states.BuildStep;
import io.harness.states.CleanupStep;
import io.harness.states.GitCloneStep;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class ExecutionRegistrar implements StepRegistrar {
  @Override
  public void register(Set<Pair<StepType, Class<? extends Step>>> stateClasses) {
    stateClasses.add(Pair.of(BuildEnvSetupStepInfo.typeInfo.getStepType(), BuildEnvSetupStep.class));
    stateClasses.add(Pair.of(CleanupStepInfo.typeInfo.getStepType(), CleanupStep.class));
    stateClasses.add(Pair.of(BuildStepInfo.typeInfo.getStepType(), BuildStep.class));
    stateClasses.add(Pair.of(GitCloneStepInfo.typeInfo.getStepType(), GitCloneStep.class));
  }
}
