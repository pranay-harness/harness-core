package io.harness.limits.checker;

import io.harness.limits.impl.model.StaticLimit;

public interface StaticLimitVicinityChecker {
  /**
   * Checks if certain percent of limit has been crossed.
   *
   * Example: you have a limit of 100 applications. And you have created 82 applications.
   * Then when
   *  percentage = 80 => return true
   *  percentage = 85 => return false
   *
   * @param percentage
   * @return
   */
  boolean hasCrossedPercentLimit(int percentage);

  StaticLimit getLimit();
}
