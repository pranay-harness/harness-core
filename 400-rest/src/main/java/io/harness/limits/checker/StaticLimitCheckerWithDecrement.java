package io.harness.limits.checker;

import io.harness.limits.Action;
import io.harness.limits.lib.StaticLimitChecker;

/**
 * Limit checker which lets you decrement the count as well. Useful for handling delete operations.
 */
public interface StaticLimitCheckerWithDecrement extends StaticLimitChecker {
  boolean decrement();

  Action getAction();
}
