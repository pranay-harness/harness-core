/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.limits.checker.rate;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.impl.model.RateLimit;

/**
 * Checks if a particular action is close to hitting the rate limit.
 * Useful for warnings before actually blocking some action.
 */
@OwnedBy(PL)
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
