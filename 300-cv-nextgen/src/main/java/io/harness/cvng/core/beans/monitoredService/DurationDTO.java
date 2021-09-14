/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans.monitoredService;

import java.time.Duration;

public enum DurationDTO {
  FOUR_HOURS(Duration.ofHours(4)),
  TWENTY_FOUR_HOURS(Duration.ofDays(1)),
  THREE_DAYS(Duration.ofDays(3)),
  SEVEN_DAYS(Duration.ofDays(7)),
  THIRTY_DAYS(Duration.ofDays(30));

  private Duration duration;

  DurationDTO(Duration duration) {
    this.duration = duration;
  }
  public Duration getDuration() {
    return duration;
  }
}
