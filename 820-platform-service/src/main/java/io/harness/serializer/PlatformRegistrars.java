package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.PlatformKryoRegistrar;
import io.harness.serializer.morphia.PlatformMorphiaRegistrar;

import com.google.common.collect.ImmutableSet;

public class PlatformRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(NotificationRegistrars.kryoRegistrars)
          .add(PlatformKryoRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(NotificationRegistrars.morphiaRegistrars)
          .add(PlatformMorphiaRegistrar.class)
          .build();
}
