package io.harness.functional.redesign.mixins.plannode;

import io.harness.pms.plan.PlanNodeProto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class PlanNodeProtoTestDeserializer extends StdDeserializer<PlanNodeProto> {
  PlanNodeProtoTestDeserializer() {
    super(PlanNodeProto.class);
  }

  @Override
  public PlanNodeProto deserialize(JsonParser p, DeserializationContext deserializationContext) throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    return PlanNodeProto.newBuilder().build();
  }
}
