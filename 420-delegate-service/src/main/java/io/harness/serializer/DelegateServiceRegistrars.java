package io.harness.serializer;

import com.google.common.collect.ImmutableSet;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.DelegateServiceKryoRegister;
import io.harness.serializer.morphia.DelegateServiceMorphiaRegistrar;
import io.harness.spring.AliasRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DelegateServiceRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(OrchestrationRegistrars.kryoRegistrars)
          .addAll(DelegateServiceBeansRegistrars.kryoRegistrars)
          .add(DelegateServiceKryoRegister.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(DelegateServiceBeansRegistrars.morphiaRegistrars)
          .addAll(OrchestrationRegistrars.morphiaRegistrars)
          .add(DelegateServiceMorphiaRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder()
          .addAll(DelegateServiceBeansRegistrars.aliasRegistrars)
          .addAll(OrchestrationRegistrars.aliasRegistrars)
          .build();
}
