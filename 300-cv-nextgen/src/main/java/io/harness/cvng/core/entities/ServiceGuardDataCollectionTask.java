package io.harness.cvng.core.entities;

import com.google.common.collect.Lists;

import io.harness.cvng.core.utils.DateTimeUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class ServiceGuardDataCollectionTask extends DataCollectionTask {
  public static final Duration SERVICE_GUARD_MAX_DATA_COLLECTION_DURATION = Duration.ofHours(2);
  private static final List<Duration> RETRY_WAIT_DURATIONS =
      Lists.newArrayList(Duration.ofSeconds(5), Duration.ofSeconds(10), Duration.ofSeconds(60), Duration.ofMinutes(5),
          Duration.ofMinutes(10), Duration.ofMinutes(30), Duration.ofHours(1));
  @Override
  public boolean shouldCreateNextTask() {
    return true;
  }

  @Override
  public boolean eligibleForRetry(Instant currentTime) {
    return getStartTime().isAfter(getDataCollectionPastTimeCutoff(currentTime));
  }

  @Override
  public Instant getNextValidAfter(Instant currentTime) {
    return currentTime.plus(RETRY_WAIT_DURATIONS.get(Math.min(this.getRetryCount(), RETRY_WAIT_DURATIONS.size() - 1)));
  }

  public Instant getDataCollectionPastTimeCutoff(Instant currentTime) {
    return DateTimeUtils.roundDownTo5MinBoundary(currentTime).minus(SERVICE_GUARD_MAX_DATA_COLLECTION_DURATION);
  }
}
