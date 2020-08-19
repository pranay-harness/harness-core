package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ng.core.delegate.sample.SimpleNotifyCallback;
import io.harness.ng.core.models.Invite;

import java.util.Set;

public class NextGenMorphiaClassesRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    // No class to register
    set.add(Invite.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("ng.core.delegate.sample.SimpleNotifyCallback", SimpleNotifyCallback.class);
  }
}
