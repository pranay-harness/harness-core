/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.limits;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.limits.configuration.NoLimitConfiguredException;
import io.harness.limits.lib.LimitChecker;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Gets the LimitChecker instance for a given action.
 */
@OwnedBy(PL)
@ParametersAreNonnullByDefault
@TargetModule(HarnessModule._955_ACCOUNT_MGMT)
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
