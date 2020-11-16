package io.harness.functional.redesign.mixins.stepparameters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.harness.state.io.StepParameters;

import java.io.IOException;

public class StepParametersTestDeserializer extends StdDeserializer<StepParameters> {
  StepParametersTestDeserializer() {
    super(StepParameters.class);
  }

  @Override
  public StepParameters deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    return new StepParameters() {};
  }
}
