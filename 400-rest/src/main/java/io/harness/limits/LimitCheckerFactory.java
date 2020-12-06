package io.harness.limits;

import io.harness.limits.configuration.NoLimitConfiguredException;
import io.harness.limits.lib.LimitChecker;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Gets the LimitChecker instance for a given action.
 */
@ParametersAreNonnullByDefault
public interface LimitCheckerFactory {
  /**
   *
   * @param action - action for which to check limit
   * @return limitChecker instance for given action
   * @throws NoLimitConfiguredException in case no limit is configured for the exception.
   *
   */
  @Nonnull LimitChecker getInstance(Action action);
}
