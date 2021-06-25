package io.harness.fieldrecaster;

import io.harness.beans.CastedField;
import io.harness.beans.RecasterMap;
import io.harness.core.Recaster;

import java.util.Map;
import org.bson.Document;

public class SimpleValueFieldRecaster implements FieldRecaster {
  @Override
  public void fromDocument(Recaster recaster, Document document, CastedField cf, Object entity) {
    recaster.getTransformer().fromDocument(entity, cf, document);
  }

  @Override
  public void fromMap(Recaster recaster, RecasterMap recasterMap, CastedField cf, Object entity) {
    recaster.getTransformer().putToEntity(entity, cf, recasterMap);
  }

  @Override
  public void toDocument(
      Recaster recaster, Object entity, CastedField cf, Document document, Map<Object, Document> involvedObjects) {
    recaster.getTransformer().toDocument(entity, cf, document);
  }

  @Override
  public void toMap(Recaster recaster, Object entity, CastedField cf, RecasterMap recasterMap) {
    recaster.getTransformer().putToMap(entity, cf, recasterMap);
  }
}
