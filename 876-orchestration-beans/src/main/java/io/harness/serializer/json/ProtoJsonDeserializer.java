package io.harness.serializer.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import lombok.SneakyThrows;

public class ProtoJsonDeserializer<T extends Message> extends JsonDeserializer<T> {
  private final Class<T> entityClass;

  @Inject
  public ProtoJsonDeserializer(Class<T> entityClass) {
    this.entityClass = entityClass;
  }

  @SneakyThrows
  @Override
  public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    ObjectNode root = p.readValueAsTree();
    Message.Builder builder = (Message.Builder) entityClass.getMethod("newBuilder").invoke(null);
    JsonFormat.parser().ignoringUnknownFields().merge(root.toString(), builder);
    return (T) builder.build();
  }
}
