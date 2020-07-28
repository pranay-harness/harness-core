package io.harness.serializer.spring;

import io.harness.spring.AliasRegistrar;
import io.harness.steps.barriers.BarrierStepParameters;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierOutcome;

import java.util.Map;

public class OrchestrationStepsAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("barrierExecutionInstance", BarrierExecutionInstance.class);
    orchestrationElements.put("barrierStepParameters", BarrierStepParameters.class);
    orchestrationElements.put("barrierOutcome", BarrierOutcome.class);
  }
}
