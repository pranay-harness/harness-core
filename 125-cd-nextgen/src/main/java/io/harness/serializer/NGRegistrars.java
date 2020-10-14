package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.NGKryoRegistrar;
import io.harness.serializer.morphia.NGMorphiaRegistrar;
import io.harness.serializer.spring.NgAliasRegistrar;
import io.harness.spring.AliasRegistrar;
import io.serializer.registrars.NGCommonsRegistrars;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NGRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(ManagerRegistrars.kryoRegistrars)
          .addAll(NGCommonsRegistrars.kryoRegistrars)
          .addAll(NGCoreRegistrars.kryoRegistrars)
          .addAll(NGPipelineRegistrars.kryoRegistrars)
          .add(NGKryoRegistrar.class)
          .build();

  public final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(ManagerRegistrars.morphiaRegistrars)
          .addAll(NGCommonsRegistrars.morphiaRegistrars)
          .addAll(NGCoreRegistrars.morphiaRegistrars)
          .addAll(NGPipelineRegistrars.morphiaRegistrars)
          .add(NGMorphiaRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder()
          .addAll(ManagerRegistrars.aliasRegistrars)
          .addAll(NGPipelineRegistrars.aliasRegistrars)
          .add(NgAliasRegistrar.class)
          .build();
}