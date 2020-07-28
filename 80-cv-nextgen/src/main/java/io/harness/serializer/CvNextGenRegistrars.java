package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.kryo.CVNGKryoRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CvNextGenRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(CvNextGenCommonsRegistrars.kryoRegistrars)
          .add(CVNGKryoRegistrar.class)
          .build();
}
