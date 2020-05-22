package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.redesign.states.http.BasicHttpStep;
import io.harness.redesign.states.shell.ShellScriptStep;
import io.harness.redesign.states.wait.WaitStep;
import io.harness.registries.registrar.StepRegistrar;
import io.harness.state.StateType;
import io.harness.state.Step;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public class WingsStepRegistrar implements StepRegistrar {
  @Override
  public void register(Set<Pair<StateType, Class<? extends Step>>> stateClasses) {
    stateClasses.add(Pair.of(BasicHttpStep.STATE_TYPE, BasicHttpStep.class));
    stateClasses.add(Pair.of(WaitStep.STATE_TYPE, WaitStep.class));
    stateClasses.add(Pair.of(ShellScriptStep.STATE_TYPE, ShellScriptStep.class));
  }
}
