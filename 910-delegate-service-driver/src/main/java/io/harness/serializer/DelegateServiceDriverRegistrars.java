package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.spring.AliasRegistrar;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DelegateServiceDriverRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(DelegateServiceBeansRegistrars.kryoRegistrars)
          .addAll(WaitEngineRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(WaitEngineRegistrars.morphiaRegistrars)
          .addAll(DelegateServiceBeansRegistrars.morphiaRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder()
          .addAll(WaitEngineRegistrars.aliasRegistrars)
          .addAll(DelegateServiceBeansRegistrars.aliasRegistrars)
          .build();
}
