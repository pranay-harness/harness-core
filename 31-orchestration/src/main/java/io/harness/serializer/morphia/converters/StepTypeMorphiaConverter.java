package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.steps.StepType;

public class StepTypeMorphiaConverter extends ProtoMessageConverter<StepType> {
  public StepTypeMorphiaConverter() {
    super(StepType.class);
  }
}
