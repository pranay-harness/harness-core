package io.harness.serializer.spring;

import io.harness.spring.AliasRegistrar;
import io.harness.steps.barriers.BarrierStepParameters;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierOutcome;
import io.harness.steps.resourcerestraint.ResourceRestraintStepParameters;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintOutcome;

import java.util.Map;

public class OrchestrationStepsAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("barrierExecutionInstance", BarrierExecutionInstance.class);
    orchestrationElements.put("barrierStepParameters", BarrierStepParameters.class);
    orchestrationElements.put("barrierOutcome", BarrierOutcome.class);
    orchestrationElements.put("resourceRestraintInstance", ResourceRestraintInstance.class);
    orchestrationElements.put("resourceRestraintOutcome", ResourceRestraintOutcome.class);
    orchestrationElements.put("resourceRestraintStepParameters", ResourceRestraintStepParameters.class);
  }
}
