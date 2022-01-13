/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.beans;

import io.harness.core.Recaster;
import io.harness.utils.RecastReflectionUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
public class EphemeralCastedField extends CastedField {
  private ParameterizedType pType;
  @Setter private Object value;
  private CastedField parent;

  public EphemeralCastedField(final ParameterizedType t, final CastedField cf, final Recaster recaster) {
    super(cf.getField(), t, recaster);
    parent = cf;
    pType = t;
    final Class rawClass = (Class) t.getRawType();
    setIsSet(RecastReflectionUtils.implementsInterface(rawClass, Set.class));
    setIsMap(RecastReflectionUtils.implementsInterface(rawClass, Map.class));
    setMapKeyType(getMapKeyClass());
    setSubType(getSubType());
  }

  public EphemeralCastedField(final Type t, final CastedField cc, final Recaster recaster) {
    super(cc.getField(), t, recaster);
    parent = cc;
  }

  @Override
  public Class getMapKeyClass() {
    return (Class) (isMap() ? pType.getActualTypeArguments()[0] : null);
  }

  @Override
  public Type getSubType() {
    return pType != null ? pType.getActualTypeArguments()[isMap() ? 1 : 0] : null;
  }

  @Override
  public Class getSubClass() {
    return toClass(getSubType());
  }
}
