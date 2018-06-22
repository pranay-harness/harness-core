package io.harness.data.structure;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import java.util.Map;

public class MapUtil {
  public static <K> void putIfNotEmpty(K key, String value, Map<K, String> map) {
    if (isNotEmpty(value)) {
      map.put(key, value);
    }
  }
}
