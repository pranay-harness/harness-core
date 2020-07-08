package io.harness.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.redesign.states.http.BasicHttpStep;
import io.harness.registries.registrar.StepRegistrar;
import io.harness.state.Step;
import io.harness.state.StepType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public class NgStepRegistrar implements StepRegistrar {
  @Override
  public void register(Set<Pair<StepType, Class<? extends Step>>> stateClasses) {
    stateClasses.add(Pair.of(BasicHttpStep.STEP_TYPE, BasicHttpStep.class));
  }
}
