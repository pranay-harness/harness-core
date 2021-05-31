package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.PmsCommonsKryoRegistrar;
import io.harness.serializer.morphia.PmsCommonsMorphiaRegistrar;
import io.harness.serializer.spring.converters.ambiance.AmbianceReadConverter;
import io.harness.serializer.spring.converters.ambiance.AmbianceWriteConverter;
import io.harness.serializer.spring.converters.facilitators.response.FacilitatorResponseReadConverter;
import io.harness.serializer.spring.converters.facilitators.response.FacilitatorResponseWriteConverter;
import io.harness.serializer.spring.converters.nodeexecution.NodeExecutionReadConverter;
import io.harness.serializer.spring.converters.nodeexecution.NodeExecutionWriteConverter;
import io.harness.serializer.spring.converters.plannode.PlanNodeProtoReadConverter;
import io.harness.serializer.spring.converters.plannode.PlanNodeProtoWriteConverter;
import io.harness.serializer.spring.converters.sdk.SdkModuleInfoReadConverter;
import io.harness.serializer.spring.converters.sdk.SdkModuleInfoWriteConverter;
import io.harness.serializer.spring.converters.steps.SdkResponseEventRequestReadConverter;
import io.harness.serializer.spring.converters.steps.SdkResponseEventRequestWriteConverter;
import io.harness.serializer.spring.converters.steptype.StepTypeReadConverter;
import io.harness.serializer.spring.converters.steptype.StepTypeWriteConverter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.springframework.core.convert.converter.Converter;

@UtilityClass
@OwnedBy(PIPELINE)
public class PmsCommonsModuleRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().add(PmsCommonsKryoRegistrar.class).build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder().add(PmsCommonsMorphiaRegistrar.class).build();

  public final ImmutableList<Class<? extends Converter<?, ?>>> springConverters = ImmutableList.of(
      StepTypeReadConverter.class, StepTypeWriteConverter.class, SdkResponseEventRequestReadConverter.class,
      SdkResponseEventRequestWriteConverter.class, AmbianceWriteConverter.class, AmbianceReadConverter.class,
      FacilitatorResponseReadConverter.class, FacilitatorResponseWriteConverter.class, PlanNodeProtoReadConverter.class,
      PlanNodeProtoWriteConverter.class, NodeExecutionReadConverter.class, NodeExecutionWriteConverter.class,
      SdkModuleInfoReadConverter.class, SdkModuleInfoWriteConverter.class);
}
