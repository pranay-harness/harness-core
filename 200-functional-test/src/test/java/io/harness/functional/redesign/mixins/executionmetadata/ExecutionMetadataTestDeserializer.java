package io.harness.functional.redesign.mixins.executionmetadata;

import io.harness.pms.contracts.plan.ExecutionMetadata;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class ExecutionMetadataTestDeserializer extends StdDeserializer<ExecutionMetadata> {
  ExecutionMetadataTestDeserializer() {
    super(ExecutionMetadata.class);
  }

  @Override
  public ExecutionMetadata deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    return ExecutionMetadata.newBuilder().build();
  }
}
