package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.plan.ExecutionMetadata;

import com.google.inject.Singleton;

@Singleton
public class ExecutionMetadataMorphiaConverter extends ProtoMessageConverter<ExecutionMetadata> {
  public ExecutionMetadataMorphiaConverter() {
    super(ExecutionMetadata.class);
  }
}
