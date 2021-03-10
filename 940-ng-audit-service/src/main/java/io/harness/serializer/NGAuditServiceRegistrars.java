package io.harness.serializer;

import io.harness.filter.serializer.FiltersRegistrars;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.morphia.NGAuditServiceMorphiaRegistrar;

import com.google.common.collect.ImmutableSet;
import io.serializer.registrars.NGCommonsRegistrars;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NGAuditServiceRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(CommonsRegistrars.kryoRegistrars)
          .addAll(NGCommonsRegistrars.kryoRegistrars)
          .addAll(PersistenceRegistrars.kryoRegistrars)
          .addAll(FiltersRegistrars.kryoRegistrars)
          .addAll(NGAuditCommonsRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(CommonsRegistrars.morphiaRegistrars)
          .addAll(NGCommonsRegistrars.morphiaRegistrars)
          .addAll(PersistenceRegistrars.morphiaRegistrars)
          .addAll(FiltersRegistrars.morphiaRegistrars)
          .addAll(NGAuditCommonsRegistrars.morphiaRegistrars)
          .add(NGAuditServiceMorphiaRegistrar.class)
          .build();
}
