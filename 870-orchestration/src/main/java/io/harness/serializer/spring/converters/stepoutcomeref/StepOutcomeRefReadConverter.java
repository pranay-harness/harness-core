package io.harness.serializer.spring.converters.stepoutcomeref;

import io.harness.pms.contracts.data.StepOutcomeRef;
import io.harness.serializer.spring.ProtoReadConverter;

public class StepOutcomeRefReadConverter extends ProtoReadConverter<StepOutcomeRef> {
  public StepOutcomeRefReadConverter() {
    super(StepOutcomeRef.class);
  }
}
