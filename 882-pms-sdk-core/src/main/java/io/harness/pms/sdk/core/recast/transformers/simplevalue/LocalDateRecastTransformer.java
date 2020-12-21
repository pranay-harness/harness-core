package io.harness.pms.sdk.core.recast.transformers.simplevalue;

import io.harness.pms.sdk.core.recast.RecastTransformer;
import io.harness.pms.sdk.core.recast.beans.CastedField;

import com.google.common.collect.ImmutableList;
import java.time.LocalDate;

public class LocalDateRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  public LocalDateRecastTransformer() {
    super(ImmutableList.of(LocalDate.class));
  }

  @Override
  public Object decode(Class<?> targetClass, Object fromObject, CastedField castedField) {
    if (fromObject == null) {
      return null;
    }

    if (fromObject instanceof LocalDate) {
      return fromObject;
    }

    throw new IllegalArgumentException("Can't convert to LocalDate from " + fromObject);
  }

  @Override
  public Object encode(Object value, CastedField castedField) {
    return value;
  }
}
