package io.harness.limits.lib;

public interface Limit {
  /**
   * Type of limit. Static, or rate based.
   */
  LimitType getLimitType();
}
