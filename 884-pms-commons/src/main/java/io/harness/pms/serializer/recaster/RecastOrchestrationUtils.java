package io.harness.pms.serializer.recaster;

import io.harness.core.Recast;
import io.harness.core.Recaster;
import io.harness.data.structure.EmptyPredicate;
import io.harness.serializer.recaster.JsonObjectRecastTransformer;
import io.harness.serializer.recaster.ParameterFieldRecastTransformer;
import io.harness.serializer.recaster.proto.ProtoEnumRecastTransformer;
import io.harness.serializer.recaster.proto.ProtoRecastTransformer;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.bson.Document;

@UtilityClass
public class RecastOrchestrationUtils {
  private static final Recast recast = new Recast();

  static {
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

  public <T> Map<String, Object> toMap(T entity) {
    Document document = recast.toDocument(entity);
    return toMapRec(document);
  }

  public static Map<String, Object> toMapRec(Document document) {
    if (document == null) {
      return null;
    }
    LinkedHashMap<String, Object> docMap = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : document.entrySet()) {
      if (entry.getValue() instanceof Document) {
        docMap.put(entry.getKey(), toMapRec((Document) entry.getValue()));
      } else {
        docMap.put(entry.getKey(), entry.getValue());
      }
    }
    return docMap;
  }

  public Document toDocumentFromJson(String json) {
    if (EmptyPredicate.isEmpty(json)) {
      return null;
    }

    return Document.parse(json);
  }

  public Map<String, Object> toMapFromJson(String json) {
    if (EmptyPredicate.isEmpty(json)) {
      return null;
    }

    return toMap(Document.parse(json));
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
}
