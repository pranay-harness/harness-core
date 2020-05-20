package io.harness.serializer.morphia;

import io.harness.cdng.core.entities.Project;
import io.harness.morphia.MorphiaRegistrar;

import java.util.Map;
import java.util.Set;

public class CDNextGenMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Project.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    // Nothing to register
  }
}
