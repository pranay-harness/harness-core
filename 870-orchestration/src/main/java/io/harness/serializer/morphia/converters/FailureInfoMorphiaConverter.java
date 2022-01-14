package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.execution.failure.FailureInfo;

import com.google.inject.Singleton;

@Singleton
public class FailureInfoMorphiaConverter extends ProtoMessageConverter<FailureInfo> {
  public FailureInfoMorphiaConverter() {
    super(FailureInfo.class);
  }
}
