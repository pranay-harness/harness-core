package io.harness.serializer;

import io.harness.serializer.kryo.LicenseBeanKryoClassesRegistrar;

import com.google.common.collect.ImmutableSet;
import io.serializer.registrars.NGCommonsRegistrars;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LicenseBeanRegistrar {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(NGCommonsRegistrars.kryoRegistrars)
          .add(LicenseBeanKryoClassesRegistrar.class)
          .build();
}
