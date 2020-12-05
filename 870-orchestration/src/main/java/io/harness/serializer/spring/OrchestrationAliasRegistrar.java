package io.harness.serializer.spring;

import io.harness.engine.executions.node.NodeExecutionTimeoutCallback;
import io.harness.spring.AliasRegistrar;

import java.util.Map;

/**
 * DO NOT CHANGE the keys. This is how track the Interface Implementations
 */
public class OrchestrationAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("nodeExecutionTimeoutCallback", NodeExecutionTimeoutCallback.class);
  }
}
