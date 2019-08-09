package io.harness.serializer.morphia;

import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.morphia.MorphiaRegistrar;

import java.util.Map;
import java.util.Set;

public class DelegateMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(ExecutionCapabilityDemander.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    final HelperPut h = (name, clazz) -> {
      map.put(pkgHarness + name, clazz);
    };

    h.put("delegate.command.CommandExecutionResult", CommandExecutionResult.class);
  }
}
