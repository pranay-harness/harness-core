package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.WaitEngineRegistrars;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.OrchestrationKryoRegister;
import io.harness.serializer.morphia.OrchestrationMorphiaRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OrchestrationRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(OrchestrationBeansRegistrars.kryoRegistrars)
          .addAll(WaitEngineRegistrars.kryoRegistrars)
          .addAll(TimeoutEngineRegistrars.kryoRegistrars)
          .add(OrchestrationKryoRegister.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(OrchestrationBeansRegistrars.morphiaRegistrars)
          .addAll(WaitEngineRegistrars.morphiaRegistrars)
          .addAll(TimeoutEngineRegistrars.morphiaRegistrars)
          .add(OrchestrationMorphiaRegistrar.class)
          .build();
}
