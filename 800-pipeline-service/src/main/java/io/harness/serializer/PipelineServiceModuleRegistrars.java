package io.harness.serializer;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.filter.serializer.morphia.FiltersMorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.DelegateServiceBeansKryoRegistrar;
import io.harness.serializer.kryo.OrchestrationVisualizationKryoRegistrar;
import io.harness.serializer.kryo.PipelineServiceKryoRegistrar;
import io.harness.serializer.morphia.NotificationClientRegistrars;
import io.harness.serializer.morphia.PMSPipelineMorphiaRegistrar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class PipelineServiceModuleRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .add(PipelineServiceKryoRegistrar.class)
          .addAll(OrchestrationStepsModuleRegistrars.kryoRegistrars)
          .add(OrchestrationVisualizationKryoRegistrar.class)
          .addAll(NGTriggerRegistrars.kryoRegistrars)
          .addAll(NotificationClientRegistrars.kryoRegistrars)
          .add(DelegateServiceBeansKryoRegistrar.class)
          .build();

  public final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(OrchestrationStepsModuleRegistrars.morphiaRegistrars)
          .add(PMSPipelineMorphiaRegistrar.class)
          .addAll(NGTriggerRegistrars.morphiaRegistrars)
          .add(FiltersMorphiaRegistrar.class)
          .addAll(NotificationClientRegistrars.morphiaRegistrars)
          .addAll(PrimaryVersionManagerRegistrars.morphiaRegistrars)
          .build();

  public final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder().addAll(OrchestrationRegistrars.morphiaConverters).build();

  public static final ImmutableList<Class<? extends Converter<?, ?>>> springConverters =
      ImmutableList.<Class<? extends Converter<?, ?>>>builder()
          .addAll(OrchestrationRegistrars.springConverters)
          .build();
}
