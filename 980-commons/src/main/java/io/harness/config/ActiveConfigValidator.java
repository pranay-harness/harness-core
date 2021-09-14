/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.config;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import java.util.Map;

public interface ActiveConfigValidator {
  default boolean isActive(Class cls, Map<String, Boolean> active) {
    boolean flag = true;
    if (isEmpty(active)) {
      return flag;
    }

    final String name = cls.getName();
    final Boolean classFlag = active.get(name);
    if (classFlag != null) {
      return classFlag.booleanValue();
    }

    int index = name.indexOf('.');
    while (index != -1) {
      final Boolean packageFlag = active.get(name.substring(0, index));
      if (packageFlag != null) {
        flag = packageFlag.booleanValue();
      }
      index = name.indexOf('.', index + 1);
    }

    return flag;
  }
}
