package io.harness.functional.redesign.mixins.failuretype;

import io.harness.pms.execution.failure.FailureInfo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class FailureInfoTestDeserializer extends StdDeserializer<FailureInfo> {
  FailureInfoTestDeserializer() {
    super(FailureInfo.class);
  }

  @Override
  public FailureInfo deserialize(JsonParser p, DeserializationContext deserializationContext) throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    return FailureInfo.newBuilder().build();
  }
}
