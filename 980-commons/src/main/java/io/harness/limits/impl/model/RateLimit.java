/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.limits.impl.model;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.lib.LimitType;
import io.harness.limits.lib.RateBasedLimit;

import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Value;

@OwnedBy(PL)
@Value
@AllArgsConstructor
public class RateLimit implements RateBasedLimit {
  private int count;
  private int duration;
  @NotNull private TimeUnit durationUnit;
  private final LimitType limitType = LimitType.RATE_LIMIT;

  // for morphia
  private RateLimit() {
    this.count = 0;
    this.duration = 0;
    this.durationUnit = null;
  }
}
