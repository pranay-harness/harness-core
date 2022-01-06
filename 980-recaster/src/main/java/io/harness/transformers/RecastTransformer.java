/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
