package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EventsFrameworkRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(PersistenceRegistrars.kryoRegistrars).build();
}
