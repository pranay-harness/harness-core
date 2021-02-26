package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.spring.converters.facilitators.response.FacilitatorResponseReadConverter;
import io.harness.serializer.spring.converters.facilitators.response.FacilitatorResponseWriteConverter;
import io.harness.serializer.spring.converters.nodeexecution.NodeExecutionReadConverter;
import io.harness.serializer.spring.converters.nodeexecution.NodeExecutionWriteConverter;
import io.harness.serializer.spring.converters.plannode.PlanNodeProtoReadConverter;
import io.harness.serializer.spring.converters.plannode.PlanNodeProtoWriteConverter;

import com.google.common.collect.ImmutableList;
import lombok.experimental.UtilityClass;
import org.springframework.core.convert.converter.Converter;

@OwnedBy(CDC)
@UtilityClass
public class PmsSdkModuleRegistrars {
  public final ImmutableList<Class<? extends Converter<?, ?>>> springConverters =
      ImmutableList.<Class<? extends Converter<?, ?>>>builder()
          .add(FacilitatorResponseReadConverter.class)
          .add(FacilitatorResponseWriteConverter.class)
          .add(PlanNodeProtoReadConverter.class)
          .add(PlanNodeProtoWriteConverter.class)
          .add(NodeExecutionReadConverter.class)
          .add(NodeExecutionWriteConverter.class)
          .addAll(PmsCommonsModuleRegistrars.springConverters)
          .build();
}
