package io.harness.serializer.morphia;

import io.harness.context.MdcGlobalContextData;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.security.SimpleEncryption;

import java.util.Map;
import java.util.Set;

public class CommonMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    // There are no classes to register from this module
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    final HelperPut h = (name, clazz) -> map.put(PKG_HARNESS + name, clazz);

    h.put("context.MdcGlobalContextData", MdcGlobalContextData.class);
    h.put("limits.impl.model.RateLimit", RateLimit.class);
    h.put("limits.impl.model.StaticLimit", StaticLimit.class);
    h.put("security.SimpleEncryption", SimpleEncryption.class);

    final HelperPut w = (name, clazz) -> map.put(PKG_WINGS + name, clazz);

    w.put("security.encryption.SimpleEncryption", SimpleEncryption.class);
  }
}
