package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.registries.registrar.StepRegistrar;
import io.harness.pms.steps.StepType;
import io.harness.redesign.states.email.EmailStep;
import io.harness.redesign.states.http.BasicHttpStep;
import io.harness.redesign.states.http.chain.BasicHttpChainStep;
import io.harness.redesign.states.shell.ShellScriptStep;
import io.harness.redesign.states.wait.WaitStep;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public class WingsStepRegistrar implements StepRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<StepType, Step>> stateClasses) {
    // Add CORE/Example/Experimental States Here
    stateClasses.add(Pair.of(BasicHttpStep.STEP_TYPE, injector.getInstance(BasicHttpStep.class)));
    stateClasses.add(Pair.of(WaitStep.STEP_TYPE, injector.getInstance(WaitStep.class)));
    stateClasses.add(Pair.of(ShellScriptStep.STEP_TYPE, injector.getInstance(ShellScriptStep.class)));
    stateClasses.add(Pair.of(EmailStep.STEP_TYPE, injector.getInstance(EmailStep.class)));
    stateClasses.add(Pair.of(BasicHttpChainStep.STEP_TYPE, injector.getInstance(BasicHttpChainStep.class)));
  }
}
