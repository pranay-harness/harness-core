package io.harness.expression;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class VariableResolverTracker {
  @Getter Map<String, Integer> usage = new HashMap<>();

  void observed(String variable) {
    usage.compute(variable, (key, value) -> value == null ? 1 : value + 1);
  }
}
