package io.harness.serializer.spring.converters.steps;

import io.harness.pms.contracts.steps.StepInfo;
import io.harness.serializer.spring.ProtoReadConverter;

public class StepInfoReadConverter extends ProtoReadConverter<StepInfo> {
  public StepInfoReadConverter() {
    super(StepInfo.class);
  }
}
