package io.harness.serializer.spring;

import io.harness.spring.AliasRegistrar;
import io.harness.yaml.core.Execution;
import io.harness.yaml.core.UseFromStage;

import java.util.Map;

/**
 * DO NOT CHANGE the keys. This is how track the Interface Implementations
 */

public class YamlBeansAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("useFromStage", UseFromStage.class);
    orchestrationElements.put("execution", Execution.class);
  }
}
