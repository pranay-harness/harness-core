package io.harness.ngpipeline.inputset.beans.yaml.serializer;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ngpipeline.inputset.beans.yaml.InputSetConfig;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.pms.yaml.ParameterField;
import io.harness.reflection.ReflectionUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

@ToBeDeleted
@Deprecated
public class InputSetConfigSerializer extends JsonSerializer<InputSetConfig> {
  @Override
  public void serializeWithType(InputSetConfig inputSetConfig, JsonGenerator jsonGenerator,
      SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
    serialize(inputSetConfig, jsonGenerator, serializers);
  }

  @Override
  public void serialize(InputSetConfig inputSetConfig, JsonGenerator jsonGenerator,
      SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeFieldName("inputSet");

    jsonGenerator.writeStartObject();

    jsonGenerator.writeObjectField("identifier", inputSetConfig.getIdentifier());
    if (EmptyPredicate.isNotEmpty(inputSetConfig.getName())) {
      jsonGenerator.writeObjectField("name", inputSetConfig.getName());
    }
    if (EmptyPredicate.isNotEmpty(inputSetConfig.getDescription())) {
      jsonGenerator.writeObjectField("description", inputSetConfig.getDescription());
    }
    if (EmptyPredicate.isNotEmpty(inputSetConfig.getTags())) {
      jsonGenerator.writeFieldName("tags");
      jsonGenerator.writeObject(inputSetConfig.getTags());
    }

    NgPipeline pipeline = inputSetConfig.getPipeline();
    jsonGenerator.writeFieldName("pipeline");
    jsonGenerator.writeStartObject();

    List<Field> fields = ReflectionUtils.getAllDeclaredAndInheritedFields(pipeline.getClass());
    for (Field field : fields) {
      Object fieldValue = ReflectionUtils.getFieldValue(pipeline, field);
      if (fieldValue == null) {
        continue;
      }

      if (ParameterField.class.isAssignableFrom(fieldValue.getClass())) {
        if (!ParameterField.isNull((ParameterField<?>) fieldValue)) {
          jsonGenerator.writeObjectField(field.getName(), fieldValue);
        }
      } else if (List.class.isAssignableFrom(fieldValue.getClass())) {
        List<?> fieldValuesList = (List<?>) fieldValue;
        jsonGenerator.writeFieldName(field.getName());
        jsonGenerator.writeStartArray();
        for (Object listItem : fieldValuesList) {
          jsonGenerator.writeObject(listItem);
        }
        jsonGenerator.writeEndArray();
      } else {
        jsonGenerator.writeObjectField(field.getName(), fieldValue);
      }
    }

    jsonGenerator.writeEndObject();

    jsonGenerator.writeEndObject();
  }

  @Override
  public Class<InputSetConfig> handledType() {
    return InputSetConfig.class;
  }
}
