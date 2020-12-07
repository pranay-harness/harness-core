package io.harness.serializer.json;

import io.harness.pms.steps.StepType;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

public class StepTypeSerializer extends StdSerializer<StepType> {
  public StepTypeSerializer(Class<StepType> t) {
    super(t);
  }

  public StepTypeSerializer() {
    this(null);
  }
  @Override
  public void serialize(StepType stepType, JsonGenerator jgen, SerializerProvider serializerProvider)
      throws IOException {
    jgen.writeStartObject();
    jgen.writeStringField("type", stepType.getType());
    jgen.writeEndObject();
  }
}
