package io.harness.app.morphia;

import io.harness.morphia.MorphiaRegistrar;

import java.util.Map;
import java.util.Set;

public class CIManagerMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {}

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    // no classes to register
  }
}
