package io.harness.yaml.core.serializer;

import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import java.io.IOException;

public class ParallelStepElementSerializer extends JsonSerializer<ParallelStepElement> {
  protected ParallelStepElementSerializer() {}

  @Override
  public void serializeWithType(ParallelStepElement parallelStepElement, JsonGenerator jsonGenerator,
      SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
    serialize(parallelStepElement, jsonGenerator, serializers);
  }

  @Override
  public void serialize(ParallelStepElement parallelStepElement, JsonGenerator jsonGenerator,
      SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeFieldName("parallel");
    jsonGenerator.writeStartArray();
    for (ExecutionWrapper executionWrapper : parallelStepElement.getSections()) {
      jsonGenerator.writeObject(executionWrapper);
    }
    jsonGenerator.writeEndArray();
    jsonGenerator.writeEndObject();
  }

  @Override
  public Class<ParallelStepElement> handledType() {
    return ParallelStepElement.class;
  }
}
