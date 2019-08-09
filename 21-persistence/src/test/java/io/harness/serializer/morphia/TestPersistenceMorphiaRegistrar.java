package io.harness.serializer.morphia;

import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.limits.impl.model.RateLimit;
import io.harness.mongo.MorphiaRegistrar;
import io.harness.persistence.MorphiaClass;

import java.util.Map;
import java.util.Set;

public class TestPersistenceMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void register(Set<Class> set) {}

  @Override
  public void register(Map<String, Class> map) {
    final HelperPut h = (name, clazz) -> {
      map.put(pkgHarness + name, clazz);
    };

    // from commons
    h.put("persistence.MorphiaOldClass", MorphiaClass.class);
    h.put("limits.impl.model.RateLimit", RateLimit.class);

    // from api-service
    h.put("globalcontex.AuditGlobalContextData", AuditGlobalContextData.class);
  }
}
