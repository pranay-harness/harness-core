package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.converters.DurationConverter;
import io.harness.persistence.converters.ObjectArrayConverter;
import io.harness.serializer.kryo.PersistenceKryoRegistrar;
import io.harness.serializer.morphia.PersistenceMorphiaRegistrar;
import io.harness.serializer.registrars.NGCommonsRegistrars;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;

@UtilityClass
public class PersistenceRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(NGCommonsRegistrars.kryoRegistrars)
          .add(PersistenceKryoRegistrar.class)
          .build();

  public static final com.google.common.collect.ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(NGCommonsRegistrars.morphiaRegistrars)
          .add(PersistenceMorphiaRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder()
          .add(DurationConverter.class)
          .add(ObjectArrayConverter.class)
          .build();
}
