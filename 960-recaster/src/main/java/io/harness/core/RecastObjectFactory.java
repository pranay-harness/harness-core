package io.harness.core;

import io.harness.beans.CastedField;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;

public interface RecastObjectFactory {
  <T> T createInstance(Class<T> clazz);

  <T> T createInstance(Class<T> clazz, Document document);

  Object createInstance(Recaster recaster, CastedField cf, Document document);

  List<Object> createList(CastedField mf);

  Map<Object, Object> createMap(CastedField mf);

  Set<Object> createSet(CastedField mf);
}
