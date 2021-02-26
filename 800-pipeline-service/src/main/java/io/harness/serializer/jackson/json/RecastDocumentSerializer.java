package io.harness.serializer.jackson.json;

import io.harness.core.Recaster;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.RecastReflectionUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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
        Object jsonFieldValue = parameterField.getJsonFieldValue();
        if (jsonFieldValue == null) {
          return null;
        }
        if (RecastReflectionUtils.implementsInterface(jsonFieldValue.getClass(), Iterable.class)) {
          traverseIterable((Iterable<Object>) jsonFieldValue);
          return jsonFieldValue;
        } else {
          return needConversion(jsonFieldValue.getClass()) ? RecastOrchestrationUtils.toDocument(jsonFieldValue)
                                                           : jsonFieldValue;
        }
      }
      traverse(value1);
    } else if (RecastReflectionUtils.implementsInterface(value.getClass(), Iterable.class)) {
      traverseIterable((Iterable<Object>) value);
    }
    return value;
  }

  private boolean needConversion(Class<?> type) {
    return !(type != null
        && (type == String.class || type == char.class || type == Character.class || type == short.class
            || type == Short.class || type == Integer.class || type == int.class || type == Long.class
            || type == long.class || type == Double.class || type == double.class || type == float.class
            || type == Float.class || type == Boolean.class || type == boolean.class || type == Byte.class
            || type == byte.class || type == Date.class || type == Locale.class || type == Class.class
            || type == UUID.class || type == URI.class || type.isEnum()));
  }
}
