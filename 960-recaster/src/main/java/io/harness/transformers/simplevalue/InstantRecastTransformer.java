package io.harness.transformers.simplevalue;

import io.harness.beans.CastedField;
import io.harness.transformers.RecastTransformer;

import com.google.common.collect.ImmutableList;
import java.time.Instant;

public class InstantRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  public InstantRecastTransformer() {
    super(ImmutableList.of(Instant.class));
  }

  @Override
  public Object decode(Class<?> targetClass, Object fromObject, CastedField castedField) {
    if (fromObject == null) {
      return null;
    }

    if (fromObject instanceof Instant) {
      return fromObject;
    }

    throw new IllegalArgumentException("Can't convert to Instant from " + fromObject);
  }

  @Override
  public Object encode(Object value, CastedField castedField) {
    return value;
  }
}
