package io.harness.pms.serializer.recaster;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.core.Recast;
import io.harness.core.Recaster;
import io.harness.data.structure.EmptyPredicate;
import io.harness.packages.HarnessPackages;
import io.harness.pms.yaml.ParameterField;
import io.harness.serializer.recaster.JsonObjectRecastTransformer;
import io.harness.serializer.recaster.ParameterFieldRecastTransformer;
import io.harness.serializer.recaster.proto.ProtoEnumRecastTransformer;
import io.harness.serializer.recaster.proto.ProtoRecastTransformer;
import io.harness.utils.RecastReflectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonParseException;
import org.bson.json.JsonWriterSettings;
import org.json.JSONException;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
@Slf4j
public class RecastOrchestrationUtils {
  private static final Recast recast = new Recast();

  static {
    recast.registerAliases(HarnessPackages.IO_HARNESS, HarnessPackages.SOFTWARE_WINGS);
    recast.addTransformer(new JsonObjectRecastTransformer());
    recast.addTransformer(new ProtoRecastTransformer());
    recast.addTransformer(new ProtoEnumRecastTransformer());
    recast.addTransformer(new ParameterFieldRecastTransformer());
  }

  public <T> Document toDocument(T entity) {
    return recast.toDocument(entity);
  }

  public <T> String toDocumentJson(T entity) {
    Document document = recast.toDocument(entity);
    return document == null ? null : document.toJson();
  }

  public Document toDocumentFromJson(String json) {
    if (EmptyPredicate.isEmpty(json)) {
      return null;
    }

    return Document.parse(json);
  }

  public <T> T fromDocument(Document document, Class<T> entityClass) {
    return recast.fromDocument(document, entityClass);
  }

  public <T> T fromDocumentJson(String json, Class<T> entityClass) {
    if (EmptyPredicate.isEmpty(json)) {
      return null;
    }

    return fromDocument(Document.parse(json), entityClass);
  }

  public Object getEncodedValue(Document doc) {
    return doc.get(Recaster.ENCODED_VALUE);
  }

  public Object setEncodedValue(Document doc, Object newValue) {
    return doc.put(Recaster.ENCODED_VALUE, newValue);
  }

  public <T> String toSimpleJson(T entity) {
    Document document = recast.toDocument(entity);
    return toSimpleJson(document);
  }

  public String toSimpleJson(Document document) {
    if (document == null) {
      return new Document().toJson();
    }
    if (document.containsKey(Recaster.RECAST_CLASS_KEY)) {
      return traverse(document);
    }
    return document.toJson(JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build());
  }

  private String traverse(Document document) {
    for (Map.Entry<String, Object> entry : document.entrySet()) {
      if (entry.getKey().equals(Recaster.RECAST_CLASS_KEY)) {
        continue;
      }
      if (entry.getKey().equals(Recaster.ENCODED_VALUE)) {
        return processEncodedValue(document);
      }
      document.put(entry.getKey(), check(entry.getValue()));
    }
    document.remove(Recaster.RECAST_CLASS_KEY);

    return document.toJson(JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build());
  }

  private List<Object> traverseIterable(Iterable<Object> collection) {
    List<Object> documentList = new ArrayList<>();
    for (Object currentValue : collection) {
      documentList.add(check(currentValue));
    }
    return documentList;
  }

  @SuppressWarnings("unchecked")
  private Object check(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Document) {
      Document value1 = (Document) value;
      if (isParameterField(value1)) {
        return handleParameterField(value1);
      }
      if (value1.containsKey(Recaster.ENCODED_VALUE)) {
        return handleEncodeValue(value1);
      }
      traverse(value1);
    } else if (RecastReflectionUtils.implementsInterface(value.getClass(), Iterable.class)) {
      return traverseIterable((Iterable<Object>) value);
    } else if (needConversion(value.getClass())) {
      value = RecastOrchestrationUtils.toDocument(value);
      traverse((Document) value);
    }
    return value;
  }

  private static Object handleEncodeValue(Document value1) {
    Object encodedValue = value1.get(Recaster.ENCODED_VALUE);
    if (encodedValue == null) {
      return null;
    }
    if (encodedValue instanceof String) {
      try {
        return check(Document.parse((String) encodedValue));
      } catch (JsonParseException ex) {
        return value1.get(Recaster.ENCODED_VALUE);
      } catch (ClassCastException ex) {
        throw new JSONException("Cannot parse encoded value");
      }
    }
    if (needConversion(encodedValue.getClass())) {
      return check(encodedValue);
    }
    return value1.get(Recaster.ENCODED_VALUE);
  }

  private static boolean isParameterField(Document value1) {
    String documentIdentifier = (String) RecastReflectionUtils.getDocumentIdentifier(value1);
    return documentIdentifier != null
        && (documentIdentifier.equals(ParameterField.class.getName())
            || documentIdentifier.equals(
                Optional.ofNullable(RecastReflectionUtils.obtainRecasterAliasValueOrNull(ParameterField.class))
                    .orElse("")));
  }

  @SuppressWarnings("unchecked")
  private Object handleParameterField(Document value1) {
    ParameterField<?> parameterField = RecastOrchestrationUtils.fromDocument(value1, ParameterField.class);
    Object jsonFieldValue = parameterField.getJsonFieldValue();
    if (jsonFieldValue == null) {
      return null;
    }
    if (RecastReflectionUtils.implementsInterface(jsonFieldValue.getClass(), Iterable.class)) {
      return traverseIterable((Iterable<Object>) jsonFieldValue);
    } else {
      return needConversion(jsonFieldValue.getClass()) ? RecastOrchestrationUtils.toDocument(jsonFieldValue)
                                                       : jsonFieldValue;
    }
  }

  @SuppressWarnings("unchecked")
  private String processEncodedValue(Document document) {
    Object encodedValue = document.get(Recaster.ENCODED_VALUE);
    if (encodedValue instanceof Collection) {
      List<Object> objects = traverseIterable((Iterable<Object>) encodedValue);
      try {
        return new ObjectMapper().writeValueAsString(objects);
      } catch (JsonProcessingException e) {
        log.error("Cannot serialize collection to Json", e);
      }
    }
    if (encodedValue instanceof Document) {
      return ((Document) encodedValue).toJson();
    }
    return encodedValue.toString();
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
