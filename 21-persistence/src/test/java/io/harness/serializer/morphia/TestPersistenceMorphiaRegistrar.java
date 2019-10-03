package io.harness.serializer.morphia;

import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.limits.impl.model.RateLimit;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.MorphiaClass;

import java.util.Map;
import java.util.Set;

public class TestPersistenceMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {}

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    final HelperPut h = (name, clazz) -> {
      map.put(PKG_HARNESS + name, clazz);
    };

    // from commons
    h.put("persistence.MorphiaOldClass", MorphiaClass.class);
    h.put("limits.impl.model.RateLimit", RateLimit.class);

    // from api-service
    h.put("globalcontex.AuditGlobalContextData", AuditGlobalContextData.class);
  }
}
