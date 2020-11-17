package io.harness.functional.redesign.mixins.facilitatorobtainment;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.harness.pms.facilitators.FacilitatorObtainment;

import java.io.IOException;

public class FacilitatorObtainmentTestDeserializer extends StdDeserializer<FacilitatorObtainment> {
  FacilitatorObtainmentTestDeserializer() {
    super(FacilitatorObtainment.class);
  }

  @Override
  public FacilitatorObtainment deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    return FacilitatorObtainment.newBuilder().build();
  }
}
