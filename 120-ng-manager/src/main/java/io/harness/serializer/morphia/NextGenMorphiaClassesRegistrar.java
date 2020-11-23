package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ng.core.delegate.sample.SimpleNotifyCallback;
import io.harness.ngtriggers.beans.entity.TriggerEventHistory;

import java.util.Set;

public class NextGenMorphiaClassesRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    // No class to register
    set.add(TriggerEventHistory.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("ng.core.delegate.sample.SimpleNotifyCallback", SimpleNotifyCallback.class);
  }
}
