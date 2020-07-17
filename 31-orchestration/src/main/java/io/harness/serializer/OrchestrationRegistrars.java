package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.WaitEngineRegistrars;
import io.harness.serializer.kryo.OrchestrationKryoRegister;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OrchestrationRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(OrchestrationBeansRegistrars.kryoRegistrars)
          .addAll(WaitEngineRegistrars.kryoRegistrars)
          .add(OrchestrationKryoRegister.class)
          .build();
}
