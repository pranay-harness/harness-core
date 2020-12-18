package io.harness.pms.sdk.core.recast;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;

public interface RecastObjectFactory {
  <T> T createInstance(Class<T> clazz);

  <T> T createInstance(Class<T> clazz, Document document);

  Object createInstance(Recaster recaster, CastedField cf, Document document);

  List createList(CastedField mf);

  Map createMap(CastedField mf);

  Set createSet(CastedField mf);
}
