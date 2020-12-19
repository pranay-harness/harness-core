package io.harness.pms.sdk.core.recast.fieldrecaster;

import io.harness.pms.sdk.core.recast.Recaster;
import io.harness.pms.sdk.core.recast.beans.CastedField;

import java.util.Map;
import org.bson.Document;

public class SimpleValueFieldRecaster implements FieldRecaster {
  @Override
  public void fromDocument(Recaster recaster, Document document, CastedField cf, Object entity) {
    recaster.getTransformer().fromDocument(entity, cf, document);
  }

  @Override
  public void toDocument(
      Recaster recaster, Object entity, CastedField cf, Document document, Map<Object, Document> involvedObjects) {
    recaster.getTransformer().toDocument(entity, cf, document);
  }
}
