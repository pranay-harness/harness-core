package io.harness.serializer.morphia.converters;

import com.google.inject.Singleton;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.steps.StepType;

@Singleton
public class StepTypeMorphiaConverter extends ProtoMessageConverter<StepType> {
  public StepTypeMorphiaConverter() {
    super(StepType.class);
  }
}
