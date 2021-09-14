/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.limits.impl.model;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.lib.LimitType;

import lombok.AllArgsConstructor;
import lombok.Value;

@OwnedBy(PL)
@Value
@AllArgsConstructor
public class StaticLimit implements io.harness.limits.lib.StaticLimit {
  private int count;
  private final LimitType limitType = LimitType.STATIC;

  public static StaticLimit copy(io.harness.limits.lib.StaticLimit limit) {
    return new StaticLimit(limit.getCount());
  }

  // for morphia
  private StaticLimit() {
    this.count = 0;
  }
}
