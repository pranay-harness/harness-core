package io.harness.serializer.morphia;

import io.harness.ng.core.entities.Project;

import java.util.Set;

public class NextGenMorphiaClassesRegistrar implements io.harness.morphia.MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Project.class);
  }

  @Override
  public void registerImplementationClasses(HelperPut h, HelperPut w) {
    // Nothing to register
  }
}
