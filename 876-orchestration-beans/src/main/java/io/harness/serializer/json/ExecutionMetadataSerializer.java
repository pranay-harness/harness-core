package io.harness.serializer.json;

import io.harness.pms.contracts.plan.ExecutionMetadata;

public class ExecutionMetadataSerializer extends ProtoJsonSerializer<ExecutionMetadata> {
  public ExecutionMetadataSerializer() {
    super(ExecutionMetadata.class);
  }
}
