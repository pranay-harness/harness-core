package io.harness.ng.core.serializer.morphia;

import io.harness.ng.core.entities.Project;

import java.util.Map;
import java.util.Set;

public class MorphiaClassesRegistrar implements io.harness.morphia.MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Project.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    // Nothing to register
  }
}
