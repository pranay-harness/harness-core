package io.harness.serializer.jackson.json;

import io.harness.core.Recaster;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.RecastReflectionUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Map;
import org.bson.Document;

public class RecastDocumentSerializer extends JsonSerializer<Document> {
  @Override
  public void serialize(Document document, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
      throws IOException {
    if (document.containsKey(Recaster.RECAST_CLASS_KEY)) {
      traverse(document);
    }
    jsonGenerator.writeRawValue(document.toJson());
  }

  private void traverse(Document document) {
    for (Map.Entry<String, Object> entry : document.entrySet()) {
      if (entry.getKey().equals(Recaster.RECAST_CLASS_KEY)) {
        continue;
      }
      document.put(entry.getKey(), check(entry.getValue()));
    }
    document.remove(Recaster.RECAST_CLASS_KEY);
  }

  private void traverseIterable(Iterable<Object> collection) {
    for (Object currentValue : collection) {
      check(currentValue);
    }
  }

  @SuppressWarnings("unchecked")
  private Object check(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Document) {
      Document value1 = (Document) value;
      if (value1.containsKey(Recaster.RECAST_CLASS_KEY)
          && value1.get(Recaster.RECAST_CLASS_KEY).equals(ParameterField.class.getName())) {
        ParameterField<?> parameterField = RecastOrchestrationUtils.fromDocument(value1, ParameterField.class);
        return RecastOrchestrationUtils.toDocument(parameterField.getJsonFieldValue());
      }
      traverse(value1);
    } else if (RecastReflectionUtils.implementsInterface(value.getClass(), Iterable.class)) {
      traverseIterable((Iterable<Object>) value);
    }
    return value;
  }
}
