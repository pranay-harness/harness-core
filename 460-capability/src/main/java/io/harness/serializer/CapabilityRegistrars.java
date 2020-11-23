package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.morphia.CapabilityMorphiaClassesRegistrar;
import io.harness.serializer.morphia.converters.CapabilityParametersMorphiaConverter;
import io.harness.serializer.morphia.converters.TestingCapabilityMorphiaConverter;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;

@UtilityClass
public class CapabilityRegistrars {
  public final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(PersistenceRegistrars.morphiaRegistrars)
          .add(CapabilityMorphiaClassesRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder()
          .add(CapabilityParametersMorphiaConverter.class)
          .add(TestingCapabilityMorphiaConverter.class)
          .build();
}
