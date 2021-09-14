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

public class ByteRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  public ByteRecastTransformer() {
    super(ImmutableList.of(byte.class, Byte.class, byte[].class, Byte[].class));
  }

  @Override
  public Object decode(final Class targetClass, final Object val, final CastedField cf) {
    if (val == null) {
      return null;
    }

    if (val.getClass().equals(targetClass)) {
      return val;
    }

    if (val instanceof Number) {
      return ((Number) val).byteValue();
    }

    if (targetClass.isArray() && val.getClass().equals(byte[].class)) {
      return convertToWrapperArray((byte[]) val);
    }
    return Byte.parseByte(val.toString());
  }

  @Override
  public Object encode(final Object value, final CastedField cf) {
    if (value instanceof Byte[]) {
      return convertToPrimitiveArray((Byte[]) value);
    }
    return value;
  }

  Object convertToPrimitiveArray(final Byte[] values) {
    final int length = values.length;
    final Object array = Array.newInstance(byte.class, length);
    for (int i = 0; i < length; i++) {
      Array.set(array, i, values[i]);
    }
    return array;
  }

  Object convertToWrapperArray(final byte[] values) {
    final int length = values.length;
    final Object array = Array.newInstance(Byte.class, length);
    for (int i = 0; i < length; i++) {
      Array.set(array, i, values[i]);
    }
    return array;
  }
}
