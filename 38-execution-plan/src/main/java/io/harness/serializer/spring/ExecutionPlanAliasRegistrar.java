package io.harness.serializer.spring;

import io.harness.executionplan.stepsdependency.bean.KeyAwareStepDependencySpec;
import io.harness.spring.AliasRegistrar;

import java.util.Map;

public class ExecutionPlanAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("keyAwareStepDependencySpec", KeyAwareStepDependencySpec.class);
  }
}
