package io.harness.serializer.morphia;

import io.harness.event.model.QueableEvent;
import io.harness.mongo.MorphiaRegistrar;

import java.util.Map;
import java.util.Set;

public class EventMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void register(Set<Class> set) {
    set.add(QueableEvent.class);
  }

  @Override
  public void register(Map<String, Class> map) {}
}
