/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
