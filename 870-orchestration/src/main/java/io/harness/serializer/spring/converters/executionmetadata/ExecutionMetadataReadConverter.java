package io.harness.serializer.spring.converters.executionmetadata;

import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.serializer.spring.ProtoReadConverter;

public class ExecutionMetadataReadConverter extends ProtoReadConverter<ExecutionMetadata> {
  public ExecutionMetadataReadConverter() {
    super(ExecutionMetadata.class);
  }
}
