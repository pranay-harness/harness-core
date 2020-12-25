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

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class WingsStepRegistrar {
  public Map<StepType, Class<? extends Step>> getEngineSteps() {
    Map<StepType, Class<? extends Step>> engineSteps = new HashMap<>();

    // Add CORE/Example/Experimental States Here
    engineSteps.put(BasicHttpStep.STEP_TYPE, BasicHttpStep.class);
    engineSteps.put(WaitStep.STEP_TYPE, WaitStep.class);
    engineSteps.put(ShellScriptStep.STEP_TYPE, ShellScriptStep.class);
    engineSteps.put(EmailStep.STEP_TYPE, EmailStep.class);
    engineSteps.put(BasicHttpChainStep.STEP_TYPE, BasicHttpChainStep.class);

    engineSteps.putAll(OrchestrationStepsModuleStepRegistrar.getEngineSteps());
    return engineSteps;
  }
}
