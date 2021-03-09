package io.harness.serializer.morphia;

import io.harness.audit.AuditEvent;
import io.harness.audit.AuditRetention;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class NGAuditCoreMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(AuditEvent.class);
    set.add(AuditRetention.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // no classes for registration
  }
}
