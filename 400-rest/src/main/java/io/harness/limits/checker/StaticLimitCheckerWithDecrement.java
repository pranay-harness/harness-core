/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.limits.checker;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.Action;
import io.harness.limits.lib.StaticLimitChecker;

/**
 * Limit checker which lets you decrement the count as well. Useful for handling delete operations.
 */
@OwnedBy(PL)
public interface StaticLimitCheckerWithDecrement extends StaticLimitChecker {
  boolean decrement();

  Action getAction();
}
