package io.harness.beans;

import io.harness.utils.RecastReflectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class RecasterMap extends LinkedHashMap<String, Object> implements Map<String, Object> {
  public static final String RECAST_CLASS_KEY = "__recast";
  public static final String ENCODED_VALUE = "__encodedValue";

  public RecasterMap() {}

  public RecasterMap(Map<? extends String, ?> m) {
    super(m);
  }

  public static RecasterMap cast(Map<String, Object> map) {
    return new RecasterMap(map);
  }

  public boolean containsIdentifier() {
    return this.containsKey(RECAST_CLASS_KEY);
  }

  public Object getIdentifier() {
    return this.get(RECAST_CLASS_KEY);
  }

  public <T> void setIdentifier(Class<T> clazz) {
    String recasterAliasValue = RecastReflectionUtils.obtainRecasterAliasValueOrNull(clazz);
    if (recasterAliasValue != null) {
      this.put(RECAST_CLASS_KEY, recasterAliasValue);
    } else {
      this.put(RECAST_CLASS_KEY, clazz.getName());
    }
  }

  public Object removeIdentifier() {
    return this.remove(RECAST_CLASS_KEY);
  }

  public boolean containsEncodedValue() {
    return this.containsKey(ENCODED_VALUE);
  }

  public Object getEncodedValue() {
    return this.get(ENCODED_VALUE);
  }

  public void setEncodedValue(Object value) {
    this.put(ENCODED_VALUE, value);
  }

  public RecasterMap append(String key, Object value) {
    this.put(key, value);
    return this;
  }
}
