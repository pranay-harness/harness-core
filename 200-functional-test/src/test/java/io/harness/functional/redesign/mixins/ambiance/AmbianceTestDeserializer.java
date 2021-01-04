package io.harness.functional.redesign.mixins.ambiance;

import io.harness.pms.contracts.ambiance.Ambiance;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class AmbianceTestDeserializer extends StdDeserializer<Ambiance> {
  AmbianceTestDeserializer() {
    super(Ambiance.class);
  }

  @Override
  public Ambiance deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    return Ambiance.newBuilder().build();
  }
}
