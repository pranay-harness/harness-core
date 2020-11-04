package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.spring.AliasRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CiExecutionRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(CiBeansRegistrars.kryoRegistrars)
          .addAll(NGPipelineRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(CiBeansRegistrars.morphiaRegistrars)
          .addAll(NGPipelineRegistrars.morphiaRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder()
          .addAll(CiBeansRegistrars.aliasRegistrars)
          .addAll(NGPipelineRegistrars.aliasRegistrars)
          .build();
}
