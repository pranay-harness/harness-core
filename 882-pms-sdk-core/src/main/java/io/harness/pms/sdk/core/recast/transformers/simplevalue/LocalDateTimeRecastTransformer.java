package io.harness.pms.sdk.core.recast.transformers.simplevalue;

import io.harness.pms.sdk.core.recast.RecastTransformer;
import io.harness.pms.sdk.core.recast.beans.CastedField;

import com.google.common.collect.ImmutableList;
import java.time.LocalDateTime;

public class LocalDateTimeRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  public LocalDateTimeRecastTransformer() {
    super(ImmutableList.of(LocalDateTime.class));
  }

  @Override
  public Object decode(Class<?> targetClass, Object fromObject, CastedField castedField) {
    if (fromObject == null) {
      return null;
    }

    if (fromObject instanceof LocalDateTime) {
      return fromObject;
    }

    throw new IllegalArgumentException("Can't convert to LocalDateTime from " + fromObject);
  }

  @Override
  public Object encode(Object value, CastedField castedField) {
    return value;
  }
}
