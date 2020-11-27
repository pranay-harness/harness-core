package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyResponse;
import io.harness.waiter.ProgressUpdate;
import io.harness.waiter.WaitInstance;

import java.util.Set;

public class WaitEngineMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(NotifyEvent.class);
    set.add(NotifyResponse.class);
    set.add(WaitInstance.class);
    set.add(ProgressUpdate.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // No Registrations required
  }
}
