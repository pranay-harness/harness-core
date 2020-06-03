package io.harness.serializer.morphia;

import io.harness.event.model.GenericEvent;
import io.harness.morphia.MorphiaRegistrar;

import java.util.Set;

public class BatchProcessingMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(GenericEvent.class);
  }

  @Override
  public void registerImplementationClasses(HelperPut h, HelperPut w) {
    // no classes to register
  }
}
