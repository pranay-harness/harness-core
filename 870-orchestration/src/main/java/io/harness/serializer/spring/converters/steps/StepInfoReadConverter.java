package io.harness.serializer.spring.converters.steps;

import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.steps.StepInfo;

public class StepInfoReadConverter extends ProtoReadConverter<StepInfo> {
  public StepInfoReadConverter() {
    super(StepInfo.class);
  }
}
