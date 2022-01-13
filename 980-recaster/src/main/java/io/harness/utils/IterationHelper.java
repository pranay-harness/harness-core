/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class IterationHelper<K, V> {
  public void loopMap(final Object x, final MapIterCallback<K, V> callback) {
    if (x == null) {
      return;
    }

    if (x instanceof Collection) {
      throw new IllegalArgumentException("call loop instead");
    }

    if (x instanceof HashMap<?, ?>) {
      if (((HashMap) x).isEmpty()) {
        return;
      }

      final HashMap<?, ?> hm = (HashMap<?, ?>) x;
      for (final Map.Entry<?, ?> e : hm.entrySet()) {
        callback.eval((K) e.getKey(), (V) e.getValue());
      }
      return;
    }
    if (x instanceof Map) {
      final Map<K, V> m = (Map<K, V>) x;
      for (final Map.Entry<K, V> entry : m.entrySet()) {
        callback.eval(entry.getKey(), entry.getValue());
      }
      return;
    }
  }

  public interface MapIterCallback<K, V> {
    /**
     * The method to call in the callback
     *
     * @param k the key from the map
     * @param v the value for the key
     */
    void eval(K k, V v);
  }
}
