/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.transformers;

import io.harness.beans.CastedField;
import io.harness.core.Recaster;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

public abstract class RecastTransformer {
  @Getter @Setter private Recaster recaster;
  @Getter private List<Class<?>> supportedTypes;

  public RecastTransformer(List<Class<?>> supportedTypes) {
    this.supportedTypes = supportedTypes;
  }

  public RecastTransformer() {}

  public abstract Object decode(Class<?> targetClass, Object fromObject, CastedField castedField);

  public Object encode(Object value) {
    return encode(value, null);
  }

  public abstract Object encode(Object value, CastedField castedField);

  public final boolean canTransform(CastedField cf) {
    return isSupported(cf.getType(), cf);
  }

  public final boolean canTransform(Class<?> c) {
    return isSupported(c, null);
  }

  public boolean isSupported(final Class<?> c, final CastedField cf) {
    return false;
  }
}
