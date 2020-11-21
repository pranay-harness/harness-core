package io.harness.functional.redesign.mixins.reftype;

import io.harness.pms.refobjects.RefType;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class RefTypeTestDeserializer extends StdDeserializer<RefType> {
  RefTypeTestDeserializer() {
    super(RefType.class);
  }

  @Override
  public RefType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    return RefType.newBuilder().build();
  }
}
