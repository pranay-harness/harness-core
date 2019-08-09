package io.harness.serializer.morphia;

import io.harness.event.model.QueableEvent;
import io.harness.morphia.MorphiaRegistrar;

import java.util.Map;
import java.util.Set;

public class EventMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(QueableEvent.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {}
}
