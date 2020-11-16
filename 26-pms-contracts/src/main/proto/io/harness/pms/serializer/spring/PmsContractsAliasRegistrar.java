package io.harness.pms.serializer.spring;

import io.harness.pms.advisers.AdviserType;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.ambiance.Level;
import io.harness.pms.execution.ExecutionMode;
import io.harness.spring.AliasRegistrar;

import java.util.Map;

public class PmsContractsAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("ambiance", Ambiance.class);
    orchestrationElements.put("level", Level.class);
    orchestrationElements.put("executionMode", ExecutionMode.class);
    orchestrationElements.put("adviserType", AdviserType.class);
  }
}