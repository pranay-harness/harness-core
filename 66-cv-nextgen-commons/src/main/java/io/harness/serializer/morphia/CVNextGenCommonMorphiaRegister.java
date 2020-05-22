package io.harness.serializer.morphia;

import io.harness.cvng.models.CVConfig;
import io.harness.morphia.MorphiaRegistrar;

import java.util.Map;
import java.util.Set;

public class CVNextGenCommonMorphiaRegister implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(CVConfig.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    // no classes to register
  }
}
