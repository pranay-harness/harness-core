package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.plan.ExecutionPlan;
import io.harness.state.execution.ExecutionInstance;
import io.harness.state.execution.ExecutionNodeInstance;

import java.util.Map;
import java.util.Set;

public class OrchestrationBeansMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(ExecutionPlan.class);
    set.add(ExecutionNodeInstance.class);
    set.add(ExecutionInstance.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    // No classes to register
  }
}
