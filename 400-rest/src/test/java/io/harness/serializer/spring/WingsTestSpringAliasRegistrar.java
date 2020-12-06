package io.harness.serializer.spring;

import io.harness.engine.interrupts.steps.SimpleStepAsyncParams;
import io.harness.spring.AliasRegistrar;

import java.util.Map;

public class WingsTestSpringAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("simpleStepAsyncParams71", SimpleStepAsyncParams.class);
  }
}
