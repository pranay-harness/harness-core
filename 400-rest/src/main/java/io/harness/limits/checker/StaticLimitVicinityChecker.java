/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.limits.checker;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.impl.model.StaticLimit;

@OwnedBy(PL)
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
