package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.pms.serializer.kryo.PmsContractsKryoRegistrar;
import io.harness.serializer.kryo.OrchestrationBeansKryoRegistrar;
import io.harness.serializer.morphia.OrchestrationBeansMorphiaRegistrar;
import io.harness.serializer.morphia.converters.SweepingOutputConverter;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;

@UtilityClass
public class OrchestrationBeansRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(PersistenceRegistrars.kryoRegistrars)
          .addAll(TimeoutEngineRegistrars.kryoRegistrars)
          .add(PmsContractsKryoRegistrar.class)
          .add(PmsSdkCoreKryoRegistrar.class)
          .add(OrchestrationBeansKryoRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(PersistenceRegistrars.morphiaRegistrars)
          .addAll(TimeoutEngineRegistrars.morphiaRegistrars)
          .add(PmsCommonsMorphiaRegistrar.class)
          .add(PmsSdkCoreMorphiaRegistrar.class)
          .add(OrchestrationBeansMorphiaRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder()
          .addAll(PersistenceRegistrars.morphiaConverters)
          .add(SweepingOutputConverter.class)
          .build();
}
