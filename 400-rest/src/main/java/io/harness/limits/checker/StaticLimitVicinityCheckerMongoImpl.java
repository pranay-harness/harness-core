/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.limits.checker;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.Counter;
import io.harness.limits.lib.StaticLimit;

import software.wings.dl.WingsPersistence;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@AllArgsConstructor
@Slf4j
public class StaticLimitVicinityCheckerMongoImpl implements StaticLimitVicinityChecker {
  private final StaticLimit limit;
  private final String key;
  private final WingsPersistence persistence;

  @Override
  public boolean hasCrossedPercentLimit(int percentage) {
    Counter counter = persistence.createQuery(Counter.class).field("key").equal(key).get();
    long used = counter.getValue();
    long allowed = limit.getCount();

    boolean crossed = used > (percentage / 100.0) * allowed;
    log.info("Static Limit Check. Used: {} , Allowed: {}, Percent: {}, Crossed Percent Limit?: {}", used, allowed,
        percentage, crossed);

    return crossed;
  }

  @Override
  public io.harness.limits.impl.model.StaticLimit getLimit() {
    return io.harness.limits.impl.model.StaticLimit.copy(limit);
  }
}
