package software.wings.service.impl.instance.stats.collector;

import com.google.common.base.Preconditions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Given the last sync time, it provides next times since then for which to collect stats.
 * See test for behaviour.
 */
@ParametersAreNonnullByDefault
class SnapshotTimeProvider {
  private long intervalInMinutes;
  private Instant creationTime;

  private Instant currentlyAt;

  SnapshotTimeProvider(Instant lastSyncTime, long durationInMinutes) {
    Objects.requireNonNull(lastSyncTime, "lastSyncTime timestamp is non-null");
    Preconditions.checkArgument(durationInMinutes > 0, "duration should be positive number");

    this.creationTime = Instant.now();
    this.currentlyAt = lastSyncTime;
    this.intervalInMinutes = durationInMinutes;
  }

  public @Nullable Instant next() {
    if (hasNext()) {
      currentlyAt = currentlyAt.plus(intervalInMinutes, ChronoUnit.MINUTES);
      return currentlyAt;
    }

    return null;
  }

  // doesn't modify state
  public boolean hasNext() {
    return currentlyAt.plus(intervalInMinutes, ChronoUnit.MINUTES).isBefore(creationTime);
  }
}
