package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.redesign.states.email.EmailStep;
import io.harness.redesign.states.http.BasicHttpStep;
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
    stateClasses.add(Pair.of(BasicHttpStep.STEP_TYPE, BasicHttpStep.class));
    stateClasses.add(Pair.of(WaitStep.STEP_TYPE, WaitStep.class));
    stateClasses.add(Pair.of(ShellScriptStep.STATE_TYPE, ShellScriptStep.class));
    stateClasses.add(Pair.of(EmailStep.STATE_TYPE, EmailStep.class));
  }
}
