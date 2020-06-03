package io.harness.beans.morphia;

import io.harness.beans.CIPipeline;
import io.harness.morphia.MorphiaRegistrar;

import java.util.Set;

public class CIBeansMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(CIPipeline.class);
  }

  @Override
  public void registerImplementationClasses(HelperPut h, HelperPut w) {
    // No classes to register
  }
}
