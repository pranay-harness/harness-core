package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.kryo.ApiServiceBeansKryoRegister;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ApiServiceBeansRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(CommonsRegistrars.kryoRegistrars)
          .add(ApiServiceBeansKryoRegister.class)
          .build();
}
