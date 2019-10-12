package io.harness.serializer.morphia;

import io.harness.event.grpc.PublishedMessage;
import io.harness.morphia.MorphiaRegistrar;

import java.util.Map;
import java.util.Set;

public class EventServerMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(PublishedMessage.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    // no classes to register
  }
}
