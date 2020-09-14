package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.OrchestrationStepsKryoRegistrar;
import io.harness.serializer.morphia.OrchestrationStepsMorphiaRegistrar;
import io.harness.serializer.spring.OrchestrationStepsAliasRegistrar;
import io.harness.spring.AliasRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OrchestrationStepsModuleRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(OrchestrationRegistrars.kryoRegistrars)
          .add(OrchestrationStepsKryoRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(PersistenceRegistrars.morphiaRegistrars)
          .addAll(OrchestrationRegistrars.morphiaRegistrars)
          .add(OrchestrationStepsMorphiaRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder()
          .addAll(OrchestrationRegistrars.aliasRegistrars)
          .add(OrchestrationStepsAliasRegistrar.class)
          .build();
}
