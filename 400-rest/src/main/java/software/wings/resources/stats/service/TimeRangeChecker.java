/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.resources.stats.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.resources.stats.model.TimeRange;

@OwnedBy(HarnessTeam.CDC)
public interface TimeRangeChecker {
  boolean istTimeInRange(TimeRange timeRange, long currentTimeMillis);
}
