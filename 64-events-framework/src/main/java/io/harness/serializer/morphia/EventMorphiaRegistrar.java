package io.harness.serializer.morphia;

import io.harness.event.model.GenericEvent;
import io.harness.morphia.MorphiaRegistrar;

import java.util.Map;
import java.util.Set;

public class EventMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(GenericEvent.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    // no classes to register
  }
}
