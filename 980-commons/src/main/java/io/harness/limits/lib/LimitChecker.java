package io.harness.limits.lib;

public interface LimitChecker {
  /**
   * Check if a particular action is allowed, and consume a permit if it is allowed.
   * If the limits need strict correctness (i.e off by one errors are not okay), the underlying implementation of this
   * should be an atomic operation in that case.
   *
   * @return whether the action is allowed or not. If the action is allowed, a unit will be consumed
   */
  boolean checkAndConsume();
}
