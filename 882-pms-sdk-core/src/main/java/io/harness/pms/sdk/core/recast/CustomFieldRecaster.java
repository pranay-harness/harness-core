package io.harness.pms.sdk.core.recast;

import java.util.Map;
import org.bson.Document;

public interface CustomFieldRecaster {
  void fromDocument();

  void toDocument(Object entity, CastedField cf, Document document, Map<Object, Document> involvedObjects);
}
