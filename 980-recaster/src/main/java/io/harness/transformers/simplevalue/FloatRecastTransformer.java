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
import java.lang.reflect.Array;
import java.util.List;

public class FloatRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  public FloatRecastTransformer() {
    super(ImmutableList.of(float.class, Float.class, float[].class, Float[].class));
  }

  @Override
  public Object decode(Class<?> targetClass, Object object, CastedField castedField) {
    if (object == null) {
      return null;
    }

    if (object instanceof Float) {
      return object;
    }

    if (object instanceof Number) {
      return ((Number) object).floatValue();
    }

    if (object instanceof List) {
      final Class<?> type = targetClass.isArray() ? targetClass.getComponentType() : targetClass;
      return convertToArray(type, (List<?>) object);
    }

    return Float.parseFloat(object.toString());
  }

  @Override
  public Object encode(Object object, CastedField castedField) {
    return object;
  }

  private Object convertToArray(final Class type, final List<?> values) {
    final Object array = Array.newInstance(type, values.size());
    try {
      return values.toArray((Object[]) array);
    } catch (Exception e) {
      for (int i = 0; i < values.size(); i++) {
        Array.set(array, i, decode(Float.class, values.get(i), null));
      }
      return array;
    }
  }
}
