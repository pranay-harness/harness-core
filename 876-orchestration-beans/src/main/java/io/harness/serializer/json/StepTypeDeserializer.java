package io.harness.serializer.json;

import io.harness.pms.contracts.steps.StepType;

public class StepTypeDeserializer extends ProtoJsonDeserializer<StepType> {
  public StepTypeDeserializer() {
    super(StepType.class);
  }
}
