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
class BatchJobScheduleTimeProvider {
  private Instant currentlyAt;
  private Instant endTime;
  private long duration;
  private ChronoUnit chronoUnit;

  BatchJobScheduleTimeProvider(Instant lastSyncTime, Instant endTime, long duration, ChronoUnit chronoUnit) {
    Objects.requireNonNull(lastSyncTime, "lastSyncTime timestamp is non-null");
    Objects.requireNonNull(endTime, "endTime  timestamp is non-null");
    Preconditions.checkArgument(duration > 0, "duration should be positive number");

    this.currentlyAt = lastSyncTime;
    this.endTime = endTime;
    this.duration = duration;
    this.chronoUnit = chronoUnit;
  }

  @Nullable
  public Instant next() {
    if (hasNext()) {
      currentlyAt = addDurationToCurrentInstant();
      return currentlyAt;
    }

    return null;
  }

  public boolean hasNext() {
    return addDurationToCurrentInstant().isBefore(endTime);
  }

  private Instant addDurationToCurrentInstant() {
    return currentlyAt.plus(duration, chronoUnit).truncatedTo(chronoUnit);
  }
}
