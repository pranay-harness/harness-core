package io.harness.functional.redesign.mixins.stepType;

import io.harness.pms.contracts.steps.StepType;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class StepTypeTestDeserializer extends StdDeserializer<StepType> {
  StepTypeTestDeserializer() {
    super(StepType.class);
  }

  @Override
  public StepType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    return StepType.newBuilder().build();
  }
}
