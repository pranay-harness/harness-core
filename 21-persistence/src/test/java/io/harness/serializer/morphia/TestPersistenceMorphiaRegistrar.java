package io.harness.serializer.morphia;

import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.limits.impl.model.RateLimit;
import io.harness.mongo.MorphiaRegistrar;
import io.harness.persistence.MorphiaClass;

import java.util.Map;

public class TestPersistenceMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void register(Map<String, Class> map) {
    // from commons
    map.put(pkgHarness + "persistence.MorphiaOldClass", MorphiaClass.class);
    map.put(pkgHarness + "limits.impl.model.RateLimit", RateLimit.class);

    // from api-service
    map.put(pkgHarness + "globalcontex.AuditGlobalContextData", AuditGlobalContextData.class);
  }
}
