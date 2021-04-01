package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.CgOrchestrationKryoRegister;
import io.harness.serializer.morphia.CgOrchestrationMorphiaRegistrar;
import io.harness.serializer.morphia.SweepingOutputConverter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;

@UtilityClass
public class CgOrchestrationRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(WaitEngineRegistrars.kryoRegistrars)
          .addAll(DelegateTasksBeansRegistrars.kryoRegistrars)
          .addAll(OrchestrationDelayRegistrars.kryoRegistrars)
          .add(CgOrchestrationKryoRegister.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(WaitEngineRegistrars.morphiaRegistrars)
          .addAll(DelegateTasksBeansRegistrars.morphiaRegistrars)
          .addAll(OrchestrationDelayRegistrars.morphiaRegistrars)
          .add(CgOrchestrationMorphiaRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder().add(SweepingOutputConverter.class).build();

  public static final List<Class<? extends Converter<?, ?>>> springConverters = ImmutableList.of();
}
