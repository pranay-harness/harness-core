package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.kryo.NGCoreKryoRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NGCoreRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(NGCoreBeansRegistrars.kryoRegistrars)
          .add(NGCoreKryoRegistrar.class)
          .build();
}
