package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.kryo.OrchestrationKryoRegister;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OrchestrationRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(OrchestrationBeansRegistrars.kryoRegistrars)
          .add(OrchestrationKryoRegister.class)
          .build();
}
