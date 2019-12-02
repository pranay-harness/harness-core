package io.harness.data.validator;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

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
    for (String key : booleanFns.keySet()) {
      Supplier<Boolean> fn = booleanFns.get(key);
      if (!fn.get()) {
        logger.info("All conditions not true. Condition returned false: {}", key);
        return false;
      }
    }

    return true;
  }
}
