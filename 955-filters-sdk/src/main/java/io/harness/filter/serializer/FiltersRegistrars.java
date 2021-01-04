package io.harness.filter.serializer;

import io.harness.filter.serializer.morphia.FiltersMorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.KryoRegistrar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.springframework.core.convert.converter.Converter;

@UtilityClass
public class FiltersRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder().add(FiltersMorphiaRegistrar.class).build();

  public static final ImmutableList<Class<? extends Converter<?, ?>>> springConverters =
      ImmutableList.<Class<? extends Converter<?, ?>>>builder().build();
}
