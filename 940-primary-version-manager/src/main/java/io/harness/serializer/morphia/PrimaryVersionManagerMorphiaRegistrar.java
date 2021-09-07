package io.harness.serializer.morphia;

import io.harness.beans.PrimaryVersion;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class PrimaryVersionManagerMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(PrimaryVersion.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}
