package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.CvNextGenCommonsBeansKryoRegistrar;
import io.harness.spring.AliasRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CvNextGenCommonsRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .add(CvNextGenCommonsBeansKryoRegistrar.class)
          .addAll(ConnectorNextGenRegistrars.kryoRegistrars)
          .addAll(CommonEntitiesRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(ConnectorNextGenRegistrars.morphiaRegistrars)
          .addAll(CommonEntitiesRegistrars.morphiaRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder()
          .addAll(ConnectorNextGenRegistrars.aliasRegistrars)
          .addAll(CommonEntitiesRegistrars.aliasRegistrars)
          .build();
}
