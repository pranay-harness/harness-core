package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.WaitEngineRegistrars;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.OrchestrationKryoRegister;
import io.harness.serializer.morphia.OrchestrationMorphiaRegistrar;
import io.harness.serializer.spring.OrchestrationAliasRegistrar;
import io.harness.spring.AliasRegistrar;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;

@UtilityClass
public class OrchestrationRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(DelegateTasksBeansRegistrars.kryoRegistrars)
          .addAll(WaitEngineRegistrars.kryoRegistrars)
          .addAll(OrchestrationBeansRegistrars.kryoRegistrars)
          .add(OrchestrationKryoRegister.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(DelegateTasksBeansRegistrars.morphiaRegistrars)
          .addAll(WaitEngineRegistrars.morphiaRegistrars)
          .addAll(OrchestrationBeansRegistrars.morphiaRegistrars)
          .add(OrchestrationMorphiaRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder()
          .addAll(PersistenceRegistrars.aliasRegistrars)
          .addAll(TimeoutEngineRegistrars.aliasRegistrars)
          .addAll(OrchestrationBeansRegistrars.aliasRegistrars)
          .add(OrchestrationAliasRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder()
          .addAll(OrchestrationBeansRegistrars.morphiaConverters)
          .build();
}
