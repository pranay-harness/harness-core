/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.beans;

import io.harness.core.Recaster;
import io.harness.utils.RecastReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class CastedClass {
  private final Class<?> clazz;
  private CastedClass superClass;
  private final List<CastedClass> interfaces = new ArrayList<>();
  private final List<CastedField> persistenceFields = new ArrayList<>();

  public CastedClass(Class<?> entityClass, Recaster recaster) {
    this.clazz = entityClass;
    explore(recaster);
  }

  private void explore(Recaster recaster) {
    Class<?> superclass = clazz.getSuperclass();
    if (superclass != null && !superclass.equals(Object.class)) {
      superClass = recaster.getCastedClass(superclass);
    }
    for (Class<?> aClass : clazz.getInterfaces()) {
      interfaces.add(recaster.getCastedClass(aClass));
    }

    for (final Field field : RecastReflectionUtils.getDeclaredAndInheritedFields(clazz, true)) {
      field.setAccessible(true);
      final int fieldModifiers = field.getModifiers();
      if (!isIgnorable(field, fieldModifiers)) {
        persistenceFields.add(new CastedField(field, clazz, recaster));
      }
    }
  }

  private boolean isIgnorable(final Field field, final int fieldModifiers) {
    return Modifier.isTransient(fieldModifiers) || field.isSynthetic() && Modifier.isTransient(fieldModifiers);
  }
}
