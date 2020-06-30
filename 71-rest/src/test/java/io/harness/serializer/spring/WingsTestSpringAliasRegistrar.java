package io.harness.serializer.spring;

import io.harness.OrchestrationBeansAliasRegistrar;
import io.harness.engine.interrupts.steps.SimpleStepAsyncParams;

import java.util.Map;

public class WingsTestSpringAliasRegistrar implements OrchestrationBeansAliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("simpleStepAsyncParams71", SimpleStepAsyncParams.class);
  }
}
