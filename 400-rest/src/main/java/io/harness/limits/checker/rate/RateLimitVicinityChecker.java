package io.harness.limits.checker.rate;

import io.harness.limits.impl.model.RateLimit;

/**
 * Checks if a particular action is close to hitting the rate limit.
 * Useful for warnings before actually blocking some action.
 */
public interface RateLimitVicinityChecker {
  /**
   * Checks if certain percent of limit has been crossed.
   *
   * Example: you have a limit of 100 actions per day. And you have done the action 82 times.
   * Then when
   *  percentage = 80 => return true
   *  percentage = 85 => return false
   *
   * @param percentage
   * @return
   */
  boolean crossed(int percentage);

  RateLimit getLimit();
}
