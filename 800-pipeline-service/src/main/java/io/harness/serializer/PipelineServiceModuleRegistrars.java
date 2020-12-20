package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.OrchestrationVisualizationKryoRegistrar;
import io.harness.serializer.morphia.PMSPipelineMorphiaRegistrar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;

@OwnedBy(CDC)
@UtilityClass
public class PipelineServiceModuleRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(OrchestrationRegistrars.kryoRegistrars)
          .add(OrchestrationVisualizationKryoRegistrar.class)
          .build();

  public final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(OrchestrationRegistrars.morphiaRegistrars)
          .add(PMSPipelineMorphiaRegistrar.class)
          .build();

  public final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder().addAll(OrchestrationRegistrars.morphiaConverters).build();

  public static final ImmutableList<Class<? extends Converter<?, ?>>> springConverters =
      ImmutableList.<Class<? extends Converter<?, ?>>>builder()
          .addAll(OrchestrationRegistrars.springConverters)
          .build();
}
