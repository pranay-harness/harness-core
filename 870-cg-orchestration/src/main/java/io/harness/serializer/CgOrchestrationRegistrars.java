package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.CgOrchestrationBeansMorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.CgOrchestrationBeansKryoRegistrar;
import io.harness.serializer.kryo.CgOrchestrationKryoRegister;
import io.harness.serializer.kryo.CommonEntitiesKryoRegistrar;
import io.harness.serializer.kryo.DelegateAgentBeansKryoRegister;
import io.harness.serializer.kryo.DelegateServiceBeansKryoRegistrar;
import io.harness.serializer.kryo.WatcherBeansKryoRegister;
import io.harness.serializer.morphia.CgOrchestrationMorphiaRegistrar;
import io.harness.serializer.morphia.CommonEntitiesMorphiaRegister;
import io.harness.serializer.morphia.SweepingOutputConverter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;

@UtilityClass
@OwnedBy(CDC)
public class CgOrchestrationRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(WaitEngineRegistrars.kryoRegistrars)
          .addAll(DelegateTasksBeansRegistrars.kryoRegistrars)
          .addAll(OrchestrationDelayRegistrars.kryoRegistrars)
          .add(CgOrchestrationKryoRegister.class)
          .add(CgOrchestrationBeansKryoRegistrar.class)
          .add(CommonEntitiesKryoRegistrar.class)
          .addAll(SMCoreRegistrars.kryoRegistrars)
          .add(DelegateServiceBeansKryoRegistrar.class)
          .add(DelegateAgentBeansKryoRegister.class)
          .add(WatcherBeansKryoRegister.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(WaitEngineRegistrars.morphiaRegistrars)
          .addAll(DelegateTasksBeansRegistrars.morphiaRegistrars)
          .addAll(OrchestrationDelayRegistrars.morphiaRegistrars)
          .add(CgOrchestrationMorphiaRegistrar.class)
          .add(CommonEntitiesMorphiaRegister.class)
          .addAll(SMCoreRegistrars.morphiaRegistrars)
          .addAll(DelegateServiceBeansRegistrars.morphiaRegistrars)
          .addAll(FeatureFlagBeansRegistrars.morphiaRegistrars)
          .addAll(EventsFrameworkRegistrars.morphiaRegistrars)
          .add(CgOrchestrationBeansMorphiaRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder().add(SweepingOutputConverter.class).build();

  public static final List<Class<? extends Converter<?, ?>>> springConverters = ImmutableList.of();
}
