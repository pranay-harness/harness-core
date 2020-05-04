package io.harness.serializer.morphia;

import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.facilitator.DefaultFacilitatorParams;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.plan.Plan;
import io.harness.state.core.fork.ForkStateParameters;

import java.util.Map;
import java.util.Set;

public class OrchestrationBeansMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Plan.class);
    set.add(NodeExecution.class);
    set.add(PlanExecution.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    final HelperPut h = (name, clazz) -> {
      map.put(PKG_HARNESS + name, clazz);
    };
    h.put("state.core.fork.ForkStateParameters", ForkStateParameters.class);
    h.put("facilitate.DefaultFacilitatorParams", DefaultFacilitatorParams.class);
  }
}
