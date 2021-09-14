/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.schedule;

import com.google.common.base.Preconditions;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Given the last sync time, it provides next times since then for which to run batch job.
 */
@ParametersAreNonnullByDefault
class BatchJobScheduleTimeProviderDesc {
  private Instant currentlyAt;
  private Instant startTime;
  private long duration;
  private ChronoUnit chronoUnit;

  BatchJobScheduleTimeProviderDesc(Instant lastSyncTime, Instant startTime, long duration, ChronoUnit chronoUnit) {
    Objects.requireNonNull(lastSyncTime, "lastSyncTime timestamp is non-null");
    Objects.requireNonNull(startTime, "startTime  timestamp is non-null");
    Preconditions.checkArgument(duration > 0, "duration should be positive number");

    this.currentlyAt = lastSyncTime;
    this.startTime = startTime;
    this.duration = duration;
    this.chronoUnit = chronoUnit;
  }

  @Nullable
  public Instant next() {
    if (hasNext()) {
      currentlyAt = removeDurationToCurrentInstant();
      return currentlyAt;
    }

    return null;
  }

  public boolean hasNext() {
    return removeDurationToCurrentInstant().isAfter(startTime);
  }

  private Instant removeDurationToCurrentInstant() {
    return currentlyAt.minus(duration, chronoUnit).truncatedTo(chronoUnit);
  }
}
