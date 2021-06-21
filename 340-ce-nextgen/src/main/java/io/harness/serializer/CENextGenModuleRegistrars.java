package io.harness.serializer;

import io.harness.ccm.serializer.CECommonsRegistrars;
import io.harness.morphia.MorphiaRegistrar;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;

@UtilityClass
public class CENextGenModuleRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .add(CENextGenMorphiaRegistrars.class)
          .addAll(PrimaryVersionManagerRegistrars.morphiaRegistrars)
          .addAll(ViewsModuleRegistrars.morphiaRegistrars)
          .addAll(CECommonsRegistrars.morphiaRegistrars)
          .addAll(FeatureFlagBeansRegistrars.morphiaRegistrars)
          .addAll(NGCoreClientRegistrars.morphiaRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder().build();

  public static final ImmutableSet<Class<? extends Converter<?, ?>>> springConverters =
      ImmutableSet.<Class<? extends Converter<?, ?>>>builder().build();
}
