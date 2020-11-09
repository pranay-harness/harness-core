package io.harness.expression;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

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
