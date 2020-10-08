package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.WaitEngineRegistrars;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.morphia.DelegateServiceBeansMorphiaRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DelegateServiceDriverRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(PersistenceRegistrars.kryoRegistrars).build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(PersistenceRegistrars.morphiaRegistrars)
          .addAll(WaitEngineRegistrars.morphiaRegistrars)
          .addAll(DelegateTasksBeansRegistrars.morphiaRegistrars)
          .add(DelegateServiceBeansMorphiaRegistrar.class)
          .build();
}
