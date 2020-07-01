package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.kryo.CommonsKryoRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommonsRegistrars {
  public static final com.google.common.collect.ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().add(CommonsKryoRegistrar.class).build();
}
