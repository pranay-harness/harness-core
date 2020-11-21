package io.harness.functional.redesign.mixins.adviserobtainment;

import io.harness.pms.advisers.AdviserObtainment;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class AdviserObtainmentTestDeserializer extends StdDeserializer<AdviserObtainment> {
  AdviserObtainmentTestDeserializer() {
    super(AdviserObtainment.class);
  }

  @Override
  public AdviserObtainment deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    return AdviserObtainment.newBuilder().build();
  }
}
