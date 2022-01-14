package io.harness.serializer;

import io.harness.filter.serializer.FiltersRegistrars;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.ConnectorBeansKryoRegistrar;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConnectorBeansRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().add(ConnectorBeansKryoRegistrar.class).build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder().addAll(FiltersRegistrars.morphiaRegistrars).build();
}
