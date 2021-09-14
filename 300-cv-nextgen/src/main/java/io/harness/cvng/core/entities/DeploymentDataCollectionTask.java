/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.entities;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class DeploymentDataCollectionTask extends DataCollectionTask {
  private static final List<Duration> RETRY_WAIT_DURATIONS = Lists.newArrayList(Duration.ofSeconds(5),
      Duration.ofSeconds(10), Duration.ofSeconds(20), Duration.ofSeconds(30), Duration.ofMinutes(1));
  @VisibleForTesting public static int MAX_RETRY_COUNT = 5;

  @Override
  public boolean shouldCreateNextTask() {
    return false;
  }

  @Override
  public boolean eligibleForRetry(Instant currentTime) {
    return getRetryCount() < MAX_RETRY_COUNT;
  }

  @Override
  public Instant getNextValidAfter(Instant currentTime) {
    return currentTime.plus(RETRY_WAIT_DURATIONS.get(Math.min(this.getRetryCount(), RETRY_WAIT_DURATIONS.size() - 1)));
  }
}
