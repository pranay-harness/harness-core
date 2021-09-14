/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.data.structure;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MapUtils {
  public static <K> void putIfNotEmpty(K key, String value, Map<K, String> map) {
    if (isNotEmpty(value)) {
      map.put(key, value);
    }
  }

  public static <K, V> Map<K, V> putToImmutable(K key, V value, Map<K, V> map) {
    try {
      map.put(key, value);
    } catch (UnsupportedOperationException ignore) {
      final HashMap<K, V> hashMap = new HashMap<>(map);
      hashMap.put(key, value);
      return hashMap;
    }
    return map;
  }
}
