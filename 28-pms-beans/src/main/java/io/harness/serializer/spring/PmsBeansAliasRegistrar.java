package io.harness.serializer.spring;

import io.harness.pms.execution.NodeExecution;
import io.harness.spring.AliasRegistrar;

import java.util.Map;

public class PmsBeansAliasRegistrar implements AliasRegistrar {
  private static final String PREFIX = "pms_";

  @Override
  public void register(Map<String, Class<?>> pmsElements) {
    pmsElements.put(PREFIX + "nodeExecution", NodeExecution.class);
  }
}
