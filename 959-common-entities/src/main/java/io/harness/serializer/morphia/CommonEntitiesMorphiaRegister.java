package io.harness.serializer.morphia;

import io.harness.entity.HarnessApiKey;
import io.harness.entity.ServiceSecretKey;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class CommonEntitiesMorphiaRegister implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(ServiceSecretKey.class);
    set.add(HarnessApiKey.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // no classes to register
  }
}
