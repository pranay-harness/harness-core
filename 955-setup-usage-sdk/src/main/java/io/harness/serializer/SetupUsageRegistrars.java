package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.morphia.SetupUsageClassesMorphiaRegistrar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.springframework.core.convert.converter.Converter;

@UtilityClass
public class SetupUsageRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(PersistenceRegistrars.kryoRegistrars).build();

  public final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(PersistenceRegistrars.morphiaRegistrars)
          .add(SetupUsageClassesMorphiaRegistrar.class)
          .build();

  public static final ImmutableList<Class<? extends Converter<?, ?>>> springConverters =
      ImmutableList.<Class<? extends Converter<?, ?>>>builder().build();
}
