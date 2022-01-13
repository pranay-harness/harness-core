/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.activityhistory.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;

@Getter
@OwnedBy(HarnessTeam.DX)
public enum TimeGroupType {
  HOUR(60 * 60 * 1000),
  DAY(24 * 60 * 60 * 1000),
  WEEK(7 * 24 * 60 * 60 * 1000);

  private long durationInMs;

  TimeGroupType(long durationInMs) {
    this.durationInMs = durationInMs;
  }
}
