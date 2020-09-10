package software.wings.service.impl.marketplace.gcp;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.common.base.Preconditions;

import io.harness.annotations.dev.OwnedBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Given the last sync time, it provides next times since then for which to send instance usage data to GCP.
 * See test for behaviour.
 */
@OwnedBy(PL)
@ParametersAreNonnullByDefault
class GCPUsageReportTimeProvider {
  private Instant currentlyAt;
  private Instant endTime;
  private long duration;
  private ChronoUnit chronoUnit;

  GCPUsageReportTimeProvider(Instant lastSyncTime, Instant endTime, long duration, ChronoUnit chronoUnit) {
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
