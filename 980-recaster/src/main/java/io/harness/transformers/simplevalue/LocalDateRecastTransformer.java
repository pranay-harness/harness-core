/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.transformers.simplevalue;

import io.harness.beans.CastedField;
import io.harness.transformers.RecastTransformer;

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
