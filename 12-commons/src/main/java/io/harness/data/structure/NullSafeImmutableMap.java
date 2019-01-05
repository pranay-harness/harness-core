package io.harness.data.structure;

public class NullSafeImmutableMap {
  public static class Builder<K, V> extends com.google.common.collect.ImmutableMap.Builder<K, V> {
    public Builder<K, V> put(K key, V value) {
      super.put(key, value);
      return this;
    }

    public Builder<K, V> putIfNotNull(K key, V value) {
      if (value != null) {
        put(key, value);
      }
      return this;
    }
  }

  public static <K, V> Builder builder() {
    return new Builder<K, V>();
  }
}
