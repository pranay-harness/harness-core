package io.harness.pms.sdk.core.recast.fieldrecaster;

import io.harness.pms.sdk.core.recast.Recaster;
import io.harness.pms.sdk.core.recast.beans.CastedField;

import java.util.Map;
import org.bson.Document;

public interface FieldRecaster {
  void fromDocument(Recaster recaster, final Document document, final CastedField cf, final Object entity);

  void toDocument(Recaster recaster, final Object entity, final CastedField cf, final Document document,
      final Map<Object, Document> involvedObjects);
}
