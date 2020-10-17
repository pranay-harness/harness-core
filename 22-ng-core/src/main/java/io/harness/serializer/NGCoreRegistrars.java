package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.NGCoreKryoRegistrar;
import io.harness.serializer.morphia.NGCoreMorphiaClassesRegistrar;
import io.serializer.registrars.NGCommonsRegistrars;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NGCoreRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(NGCommonsRegistrars.kryoRegistrars)
          .addAll(NGCoreBeansRegistrars.kryoRegistrars)
          .addAll(PersistenceRegistrars.kryoRegistrars)
          .addAll(ApiServiceBeansRegistrars.kryoRegistrars)
          .add(NGCoreKryoRegistrar.class)
          .build();

  public final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(NGCommonsRegistrars.morphiaRegistrars)
          .addAll(PersistenceRegistrars.morphiaRegistrars)
          .add(NGCoreMorphiaClassesRegistrar.class)
          .build();
}
