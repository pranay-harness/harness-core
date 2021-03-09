package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.morphia.NGAuditCoreMorphiaRegistrar;

import com.google.common.collect.ImmutableSet;
import io.serializer.registrars.NGCommonsRegistrars;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NGAuditCoreRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(CommonsRegistrars.kryoRegistrars)
          .addAll(NGCommonsRegistrars.kryoRegistrars)
          .addAll(PersistenceRegistrars.kryoRegistrars)
          .addAll(NGAuditCommonsRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(CommonsRegistrars.morphiaRegistrars)
          .addAll(NGCommonsRegistrars.morphiaRegistrars)
          .addAll(PersistenceRegistrars.morphiaRegistrars)
          .addAll(NGAuditCommonsRegistrars.morphiaRegistrars)
          .add(NGAuditCoreMorphiaRegistrar.class)
          .build();
}
