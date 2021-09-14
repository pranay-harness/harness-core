/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
