/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.transformers.simplevalue;

import io.harness.beans.CastedField;
import io.harness.transformers.RecastTransformer;

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
