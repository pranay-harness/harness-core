package io.harness.functional.redesign.mixins.advisertype;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.harness.pms.advisers.AdviserType;

import java.io.IOException;

public class AdviserTypeTestDeserializer extends StdDeserializer<AdviserType> {
  AdviserTypeTestDeserializer() {
    super(AdviserType.class);
  }

  @Override
  public AdviserType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    return AdviserType.newBuilder().build();
  }
}
