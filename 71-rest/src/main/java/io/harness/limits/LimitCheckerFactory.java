package io.harness.limits;

import io.harness.limits.lib.LimitChecker;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Gets the LimitChecker instance for a given action.
 */
@ParametersAreNonnullByDefault
public interface LimitCheckerFactory {
  @Nonnull LimitChecker getInstance(Action action);
}
