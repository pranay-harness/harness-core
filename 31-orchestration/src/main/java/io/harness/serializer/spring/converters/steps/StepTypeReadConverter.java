package io.harness.serializer.spring.converters.steps;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.steps.StepType;
import org.springframework.data.convert.ReadingConverter;

import static io.harness.annotations.dev.HarnessTeam.CDC;

@OwnedBy(CDC)
@ReadingConverter
public class StepTypeReadConverter extends ProtoReadConverter<StepType> {
  public StepTypeReadConverter() {
    super(StepType.class);
  }
}
