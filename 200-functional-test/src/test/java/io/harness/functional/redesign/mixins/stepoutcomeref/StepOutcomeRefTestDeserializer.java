package io.harness.functional.redesign.mixins.stepoutcomeref;

import io.harness.pms.data.StepOutcomeRef;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class StepOutcomeRefTestDeserializer extends StdDeserializer<StepOutcomeRef> {
  StepOutcomeRefTestDeserializer() {
    super(StepOutcomeRef.class);
  }

  @Override
  public StepOutcomeRef deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    return StepOutcomeRef.newBuilder().build();
  }
}
