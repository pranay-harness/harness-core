package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.PersistenceKryoRegistrar;
import io.harness.serializer.morphia.PersistenceMorphiaRegistrar;
import io.harness.serializer.spring.PersistenceAliasRegistrar;
import io.harness.spring.AliasRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PersistenceRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(CommonsRegistrars.kryoRegistrars)
          .add(PersistenceKryoRegistrar.class)
          .build();

  public static final com.google.common.collect.ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(CommonsRegistrars.morphiaRegistrars)
          .add(PersistenceMorphiaRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder().add(PersistenceAliasRegistrar.class).build();
}
