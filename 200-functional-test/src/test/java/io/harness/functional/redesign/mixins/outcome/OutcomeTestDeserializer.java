package io.harness.functional.redesign.mixins.outcome;

import io.harness.beans.ExecutionStatus;
import io.harness.data.Outcome;

import software.wings.api.HttpStateExecutionData;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class OutcomeTestDeserializer extends StdDeserializer<Outcome> {
  OutcomeTestDeserializer() {
    super(Outcome.class);
  }

  @Override
  public Outcome deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    final String httpExecutionDataParam = "httpMethod";
    JsonNode node = p.getCodec().readTree(p);
    if (node.hasNonNull(httpExecutionDataParam)) {
      return HttpStateExecutionData.builder()
          .status(ExecutionStatus.valueOf(node.get("status").asText()))
          .httpMethod(node.get("httpMethod").asText())
          .build();
    }
    return null;
  }
}
