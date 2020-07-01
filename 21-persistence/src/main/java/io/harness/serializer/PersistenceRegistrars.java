package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.kryo.PersistenceRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PersistenceRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().add(PersistenceRegistrar.class).build();
}
