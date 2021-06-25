package io.harness.fieldrecaster;

import io.harness.beans.CastedField;
import io.harness.beans.RecasterMap;
import io.harness.core.Recaster;

import java.util.Map;
import org.bson.Document;

public interface FieldRecaster {
  void fromDocument(Recaster recaster, Document document, CastedField cf, Object entity);

  void fromMap(Recaster recaster, RecasterMap recasterMap, CastedField cf, Object entity);

  void toDocument(
      Recaster recaster, Object entity, CastedField cf, Document document, Map<Object, Document> involvedObjects);

  void toMap(Recaster recaster, Object entity, CastedField cf, RecasterMap recasterMap);
}
