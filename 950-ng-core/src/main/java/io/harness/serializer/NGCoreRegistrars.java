package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.NGCoreKryoRegistrar;
import io.harness.serializer.morphia.NGCoreMorphiaClassesRegistrar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.springframework.core.convert.converter.Converter;

@UtilityClass
public class NGCoreRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(NGCoreBeansRegistrars.kryoRegistrars)
          .addAll(PersistenceRegistrars.kryoRegistrars)
          .addAll(ConnectorBeansRegistrars.kryoRegistrars)
          .add(NGCoreKryoRegistrar.class)
          .build();

  public final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(NGCoreBeansRegistrars.morphiaRegistrars)
          .addAll(SetupUsageRegistrars.morphiaRegistrars)
          .addAll(ConnectorBeansRegistrars.morphiaRegistrars)
          .add(NGCoreMorphiaClassesRegistrar.class)
          .build();

  public static final ImmutableList<Class<? extends Converter<?, ?>>> springConverters =
      ImmutableList.<Class<? extends Converter<?, ?>>>builder().build();
}
