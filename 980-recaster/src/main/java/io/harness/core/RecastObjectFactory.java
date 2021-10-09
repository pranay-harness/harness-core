package io.harness.core;

import io.harness.beans.CastedField;
import io.harness.beans.RecasterMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RecastObjectFactory {
  <T> T createInstance(Class<T> clazz);

  <T> T createInstance(Class<T> clazz, RecasterMap recasterMap);

  Object createInstance(Recaster recaster, CastedField cf, RecasterMap recasterMap);

  List<Object> createList(CastedField mf);

  Map<Object, Object> createMap(CastedField mf);

  Set<Object> createSet(CastedField mf);
}
