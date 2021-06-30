package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.morphia.OutboxEventMorphiaRegistrar;
import io.harness.serializer.registrars.NGCommonsRegistrars;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class OutboxEventRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(NGCommonsRegistrars.kryoRegistrars)
          .addAll(PersistenceRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(NGCommonsRegistrars.morphiaRegistrars)
          .addAll(PersistenceRegistrars.morphiaRegistrars)
          .add(OutboxEventMorphiaRegistrar.class)
          .build();
}
