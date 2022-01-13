/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
