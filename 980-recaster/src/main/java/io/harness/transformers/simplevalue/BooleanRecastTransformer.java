/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.transformers.simplevalue;

import io.harness.beans.CastedField;
import io.harness.transformers.RecastTransformer;
import io.harness.utils.RecastReflectionUtils;

import com.google.common.collect.ImmutableList;
import java.util.List;

public class BooleanRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  public BooleanRecastTransformer() {
    super(ImmutableList.of(boolean.class, Boolean.class, boolean[].class, Boolean[].class));
  }

  @Override
  public Object encode(Object value, CastedField castedField) {
    return value;
  }

  @Override
  public Object decode(final Class targetClass, final Object val, final CastedField castedField) {
    if (val == null) {
      return null;
    }

    if (val instanceof Boolean) {
      return val;
    }

    // handle the case for things like the ok field
    if (val instanceof Number) {
      return ((Number) val).intValue() != 0;
    }

    if (val instanceof List) {
      final Class<?> type = targetClass.isArray() ? targetClass.getComponentType() : targetClass;
      return RecastReflectionUtils.convertToArray(type, (List<?>) val);
    }

    return Boolean.parseBoolean(val.toString());
  }
}
