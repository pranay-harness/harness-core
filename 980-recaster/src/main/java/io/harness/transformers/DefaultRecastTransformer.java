/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
