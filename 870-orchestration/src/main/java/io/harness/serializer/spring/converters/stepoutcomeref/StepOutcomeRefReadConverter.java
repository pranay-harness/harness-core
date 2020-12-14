package io.harness.serializer.spring.converters.stepoutcomeref;

import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.contracts.data.StepOutcomeRef;

public class StepOutcomeRefReadConverter extends ProtoReadConverter<StepOutcomeRef> {
  public StepOutcomeRefReadConverter() {
    super(StepOutcomeRef.class);
  }
}
