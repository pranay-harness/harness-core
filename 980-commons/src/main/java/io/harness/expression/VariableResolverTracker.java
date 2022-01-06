/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.expression;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

public class VariableResolverTracker {
  @Getter Map<String, Map<Object, Integer>> usage = new HashMap<>();

  private static String MASK = "***";

  void observed(String variable, Object value) {
    final Map<Object, Integer> integerMap = usage.computeIfAbsent(variable, key -> new HashMap<>());

    if (value instanceof SecretString) {
      value = MASK;
    }

    integerMap.compute(value, (key, count) -> count == null ? 1 : count + 1);
  }
}
