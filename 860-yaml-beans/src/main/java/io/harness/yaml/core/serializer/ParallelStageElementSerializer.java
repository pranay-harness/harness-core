package io.harness.yaml.core.serializer;

import io.harness.yaml.core.ParallelStageElement;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import java.io.IOException;

public class ParallelStageElementSerializer extends JsonSerializer<ParallelStageElement> {
  @Override
  public void serializeWithType(ParallelStageElement parallelStageElement, JsonGenerator jsonGenerator,
      SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
    serialize(parallelStageElement, jsonGenerator, serializers);
  }

  @Override
  public void serialize(ParallelStageElement parallelStageElement, JsonGenerator jsonGenerator,
      SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeFieldName("parallel");
    jsonGenerator.writeStartArray();
    for (StageElementWrapper stageElementWrapper : parallelStageElement.getSections()) {
      jsonGenerator.writeObject(stageElementWrapper);
    }
    jsonGenerator.writeEndArray();
    jsonGenerator.writeEndObject();
  }

  @Override
  public Class<ParallelStageElement> handledType() {
    return ParallelStageElement.class;
  }
}
