package io.harness.serializer.morphia;

import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.mongo.MorphiaRegistrar;

import java.util.Map;

public class PersistenceMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void register(Map<String, Class> map) {
    // from commons
    map.put(harnessPackage + "limits.impl.model.StaticLimit", StaticLimit.class);
    map.put(harnessPackage + "limits.impl.model.RateLimit", RateLimit.class);

    // from api-service
    map.put(harnessPackage + "globalcontex.AuditGlobalContextData", AuditGlobalContextData.class);
  }
}
