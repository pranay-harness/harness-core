package io.harness.transformers;

import io.harness.beans.CastedField;

public class DefaultRecastTransformer extends RecastTransformer {
  @Override
  public Object decode(Class<?> targetClass, Object fromObject, CastedField castedField) {
    return fromObject;
  }

  @Override
  public Object encode(Object value, CastedField castedField) {
    return value;
  }

  @Override
  public boolean isSupported(final Class<?> c, final CastedField cf) {
    return true;
  }
}
