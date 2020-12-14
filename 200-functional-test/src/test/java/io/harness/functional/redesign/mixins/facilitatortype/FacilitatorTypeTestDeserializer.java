package io.harness.functional.redesign.mixins.facilitatortype;

import io.harness.pms.contracts.facilitators.FacilitatorType;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class FacilitatorTypeTestDeserializer extends StdDeserializer<FacilitatorType> {
  FacilitatorTypeTestDeserializer() {
    super(FacilitatorType.class);
  }

  @Override
  public FacilitatorType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    return FacilitatorType.newBuilder().build();
  }
}
