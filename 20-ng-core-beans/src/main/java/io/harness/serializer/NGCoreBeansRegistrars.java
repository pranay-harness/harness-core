package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.kryo.NGCoreBeansKryoRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NGCoreBeansRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().add(NGCoreBeansKryoRegistrar.class).build();
}
