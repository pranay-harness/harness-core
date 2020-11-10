package io.harness.pms.serializer.spring;

import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.ambiance.Level;
import io.harness.spring.AliasRegistrar;

import java.util.Map;

public class PmsContractsAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("ambiance", Ambiance.class);
    orchestrationElements.put("level", Level.class);
  }
}