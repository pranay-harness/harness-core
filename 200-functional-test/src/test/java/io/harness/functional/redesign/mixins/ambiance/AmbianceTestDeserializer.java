package io.harness.functional.redesign.mixins.ambiance;

import io.harness.ambiance.Ambiance;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class AmbianceTestDeserializer extends StdDeserializer<Ambiance> {
  AmbianceTestDeserializer() {
    super(Ambiance.class);
  }

  @Override
  public Ambiance deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    return new Ambiance(new HashMap<>(), new ArrayList<>(), "");
  }
}
