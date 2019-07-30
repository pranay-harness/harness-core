package io.harness.serializer.morphia;

import io.harness.delegate.command.CommandExecutionResult;
import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.mongo.MorphiaRegistrar;
import io.harness.security.SimpleEncryption;

import java.util.Map;

public class PersistenceMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void register(Map<String, Class> map) {
    // from commons
    map.put(pkgHarness + "limits.impl.model.StaticLimit", StaticLimit.class);
    map.put(pkgHarness + "limits.impl.model.RateLimit", RateLimit.class);
    map.put(pkgHarness + "security.SimpleEncryption", SimpleEncryption.class);

    // from api-service
    map.put(pkgHarness + "globalcontex.AuditGlobalContextData", AuditGlobalContextData.class);

    // from delegate-task-beans
    map.put(pkgHarness + "delegate.command.CommandExecutionResult", CommandExecutionResult.class);
  }
}
