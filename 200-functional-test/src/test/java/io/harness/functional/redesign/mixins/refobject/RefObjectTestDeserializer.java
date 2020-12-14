package io.harness.functional.redesign.mixins.refobject;

import io.harness.pms.contracts.refobjects.RefObject;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class RefObjectTestDeserializer extends StdDeserializer<RefObject> {
  RefObjectTestDeserializer() {
    super(RefObject.class);
  }

  @Override
  public RefObject deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    return RefObject.newBuilder().build();
  }
}
