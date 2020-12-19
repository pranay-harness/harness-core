package io.harness.pms.sdk.core.recast.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.bson.BSONObject;

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
    if (x instanceof BSONObject) {
      final BSONObject m = (BSONObject) x;
      for (final String k : m.keySet()) {
        callback.eval((K) k, (V) m.get(k));
      }
    }
  }

  public abstract static class MapIterCallback<K, V> {
    /**
     * The method to call in the callback
     *
     * @param k the key from the map
     * @param v the value for the key
     */
    public abstract void eval(K k, V v);
  }
}
