package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.execution.failure.FailureInfo;
import io.harness.pms.steps.StepType;

import com.google.inject.Singleton;

@Singleton
public class FailureInfoMorphiaConverter extends ProtoMessageConverter<FailureInfo> {
  public FailureInfoMorphiaConverter() {
    super(FailureInfo.class);
  }
}
