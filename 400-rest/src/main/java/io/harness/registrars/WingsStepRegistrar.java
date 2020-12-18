package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.redesign.states.email.EmailStep;
import io.harness.redesign.states.http.BasicHttpStep;
import io.harness.redesign.states.http.chain.BasicHttpChainStep;
import io.harness.redesign.states.shell.ShellScriptStep;
import io.harness.redesign.states.wait.WaitStep;

import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class WingsStepRegistrar {
  public Map<StepType, Step> getEngineSteps(Injector injector) {
    Map<StepType, Step> engineSteps = new HashMap<>();

    // Add CORE/Example/Experimental States Here
    engineSteps.put(BasicHttpStep.STEP_TYPE, injector.getInstance(BasicHttpStep.class));
    engineSteps.put(WaitStep.STEP_TYPE, injector.getInstance(WaitStep.class));
    engineSteps.put(ShellScriptStep.STEP_TYPE, injector.getInstance(ShellScriptStep.class));
    engineSteps.put(EmailStep.STEP_TYPE, injector.getInstance(EmailStep.class));
    engineSteps.put(BasicHttpChainStep.STEP_TYPE, injector.getInstance(BasicHttpChainStep.class));

    engineSteps.putAll(OrchestrationStepsModuleStepRegistrar.getEngineSteps(injector));
    return engineSteps;
  }
}
