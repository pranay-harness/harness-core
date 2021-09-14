/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
