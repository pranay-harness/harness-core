/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.transformers.simplevalue;

import io.harness.beans.CastedField;
import io.harness.transformers.RecastTransformer;
import io.harness.utils.RecastReflectionUtils;

import com.google.common.collect.ImmutableList;
import java.util.List;

public class StringRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  public StringRecastTransformer() {
    super(ImmutableList.of(String.class, String[].class));
  }

  @Override
  public Object encode(Object value, CastedField castedField) {
    return value;
  }

  @Override
  public Object decode(final Class targetClass, final Object fromDBObject, final CastedField castedField) {
    if (fromDBObject == null) {
      return null;
    }

    if (targetClass.equals(fromDBObject.getClass())) {
      return fromDBObject;
    }

    if (fromDBObject instanceof List) {
      final Class<?> type = targetClass.isArray() ? targetClass.getComponentType() : targetClass;
      return RecastReflectionUtils.convertToArray(type, (List<?>) fromDBObject);
    }

    if (targetClass.equals(String[].class)) {
      return new String[] {fromDBObject.toString()};
    }

    return fromDBObject.toString();
  }
}
