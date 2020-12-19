package io.harness.pms.sdk.core.recast.transformers;

import io.harness.pms.sdk.core.recast.RecastTransformer;
import io.harness.pms.sdk.core.recast.beans.CastedField;

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
  protected boolean isSupported(final Class<?> c, final CastedField cf) {
    return true;
  }
}
