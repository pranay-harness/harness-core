/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.data.validator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConditionsValidator {
  @Value
  public static class Condition {
    private String label;

    // expectation should return be true for condition to be considered met.
    private Supplier<Boolean> expectation;
  }

  Map<String, Supplier<Boolean>> expectations = new HashMap<>();

  public void addCondition(Condition condition) {
    expectations.put(condition.label, condition.expectation);
  }

  public boolean allConditionsSatisfied() {
    return allTrue(expectations);
  }

  private static boolean allTrue(Map<String, Supplier<Boolean>> booleanFns) {
    for (Map.Entry<String, Supplier<Boolean>> entry : booleanFns.entrySet()) {
      Supplier<Boolean> fn = entry.getValue();
      if (!fn.get()) {
        log.info("All conditions not true. Condition returned false: {}", entry.getKey());
        return false;
      }
    }

    return true;
  }
}
