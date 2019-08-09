package io.harness.serializer.morphia;

import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.security.SimpleEncryption;

import java.util.Map;
import java.util.Set;

public class CommonMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {}

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    final HelperPut h = (name, clazz) -> {
      map.put(pkgHarness + name, clazz);
    };

    h.put("limits.impl.model.StaticLimit", StaticLimit.class);
    h.put("limits.impl.model.RateLimit", RateLimit.class);
    h.put("security.SimpleEncryption", SimpleEncryption.class);

    final HelperPut w = (name, clazz) -> {
      map.put(pkgWings + name, clazz);
    };

    w.put("security.encryption.SimpleEncryption", SimpleEncryption.class);
  }
}
