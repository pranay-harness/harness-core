package io.harness.serializer.json;

import io.harness.pms.contracts.execution.failure.FailureInfo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;

public class FailureInfoDeserializer extends JsonDeserializer<FailureInfo> {
  @Override
  public FailureInfo deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    ObjectNode root = p.readValueAsTree();
    FailureInfo.Builder builder = FailureInfo.newBuilder();
    JsonFormat.parser().ignoringUnknownFields().merge(root.toString(), builder);
    return builder.build();
  }
}
