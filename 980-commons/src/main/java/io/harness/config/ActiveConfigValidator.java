/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
