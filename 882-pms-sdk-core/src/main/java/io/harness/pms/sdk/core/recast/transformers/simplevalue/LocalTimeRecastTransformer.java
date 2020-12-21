package io.harness.pms.sdk.core.recast.transformers.simplevalue;

import io.harness.pms.sdk.core.recast.RecastTransformer;
import io.harness.pms.sdk.core.recast.beans.CastedField;

import com.google.common.collect.ImmutableList;
import java.time.LocalTime;

public class LocalTimeRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  public LocalTimeRecastTransformer() {
    super(ImmutableList.of(LocalTime.class));
  }

  @Override
  public Object decode(Class<?> targetClass, Object fromObject, CastedField castedField) {
    if (fromObject == null) {
      return null;
    }

    if (fromObject instanceof LocalTime) {
      return fromObject;
    }
    throw new IllegalArgumentException("Can't convert to LocalTime from " + fromObject);
  }

  @Override
  public Object encode(Object value, CastedField castedField) {
    return value;
  }
}
