package io.harness.serializer;

import io.harness.delegate.serializer.DelegateTasksRegistrars;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.OrchestrationKryoRegister;
import io.harness.serializer.morphia.OrchestrationMorphiaRegistrar;
import io.harness.serializer.morphia.converters.*;
import io.harness.serializer.spring.OrchestrationAliasRegistrar;
import io.harness.spring.AliasRegistrar;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;

@UtilityClass
public class OrchestrationRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(DelegateTasksRegistrars.kryoRegistrars)
          .addAll(WaitEngineRegistrars.kryoRegistrars)
          .addAll(OrchestrationBeansRegistrars.kryoRegistrars)
          .add(OrchestrationKryoRegister.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(DelegateTasksRegistrars.morphiaRegistrars)
          .addAll(WaitEngineRegistrars.morphiaRegistrars)
          .addAll(OrchestrationBeansRegistrars.morphiaRegistrars)
          .add(OrchestrationMorphiaRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder()
          .addAll(DelegateTasksRegistrars.aliasRegistrars)
          .addAll(WaitEngineRegistrars.aliasRegistrars)
          .addAll(OrchestrationBeansRegistrars.aliasRegistrars)
          .add(OrchestrationAliasRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder()
          .addAll(OrchestrationBeansRegistrars.morphiaConverters)
          .add(AmbianceMorphiaConverter.class)
          .add(LevelMorphiaConverter.class)
          .add(StepTypeMorphiaConverter.class)
          .add(RefObjectMorphiaConverter.class)
          .add(AdviserTypeMorphiaConverter.class)
          .add(AdviserObtainmentMorphiaConverter.class)
          .add(FacilitatorTypeMorphiaConverter.class)
          .add(FacilitatorObtainmentMorphiaConverter.class)
          .add(RefTypeMorphiaConverter.class)
          .build();
}
