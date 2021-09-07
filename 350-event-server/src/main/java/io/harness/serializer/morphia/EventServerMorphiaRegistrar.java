package io.harness.serializer.morphia;

import io.harness.ccm.commons.entities.events.PublishedMessage;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class EventServerMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(PublishedMessage.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // no classes to register
  }
}
