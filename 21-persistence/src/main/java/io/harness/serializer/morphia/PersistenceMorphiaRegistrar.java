package io.harness.serializer.morphia;

import io.harness.delegate.command.CommandExecutionResult;
import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.globalcontex.PurgeGlobalContextData;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.mongo.MorphiaRegistrar;
import io.harness.security.SimpleEncryption;

import java.util.Map;

public class PersistenceMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void register(Map<String, Class> map) {
    final HelperPut h = (name, clazz) -> {
      map.put(pkgHarness + name, clazz);
    };

    // from commons
    h.put("limits.impl.model.StaticLimit", StaticLimit.class);
    h.put("limits.impl.model.RateLimit", RateLimit.class);
    h.put("security.SimpleEncryption", SimpleEncryption.class);

    // from api-service
    h.put("globalcontex.AuditGlobalContextData", AuditGlobalContextData.class);
    h.put("globalcontex.PurgeGlobalContextData", PurgeGlobalContextData.class);

    // from delegate-task-beans
    h.put("delegate.command.CommandExecutionResult", CommandExecutionResult.class);
  }
}
