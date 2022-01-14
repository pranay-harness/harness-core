package io.harness.serializer.json;

import io.harness.pms.contracts.steps.StepType;

public class StepTypeSerializer extends ProtoJsonSerializer<StepType> {
  public StepTypeSerializer(Class<StepType> t) {
    super(t);
  }

  public StepTypeSerializer() {
    this(null);
  }
}
