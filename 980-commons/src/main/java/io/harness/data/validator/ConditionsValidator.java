/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
