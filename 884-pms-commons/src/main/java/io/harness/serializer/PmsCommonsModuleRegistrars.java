package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.PmsCommonsKryoRegistrar;
import io.harness.serializer.morphia.PmsCommonsMorphiaRegistrar;
import io.harness.serializer.spring.converters.steptype.StepTypeReadConverter;
import io.harness.serializer.spring.converters.steptype.StepTypeWriteConverter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.springframework.core.convert.converter.Converter;

@UtilityClass
public class PmsCommonsModuleRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().add(PmsCommonsKryoRegistrar.class).build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder().add(PmsCommonsMorphiaRegistrar.class).build();

  public final ImmutableList<Class<? extends Converter<?, ?>>> springConverters =
      ImmutableList.of(StepTypeReadConverter.class, StepTypeWriteConverter.class);
}
