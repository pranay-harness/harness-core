package software.wings.service.intfc.instance.stats;

import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;
import software.wings.resources.stats.model.InstanceTimeline;

import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This service is used to render the timelines and provide aggregates on user dashboard.
 */
@ParametersAreNonnullByDefault
public interface InstanceStatService {
  /**
   * Save an instance of {@code InstanceStatsSnapshot}
   */
  boolean save(InstanceStatsSnapshot stats);

  /**
   * Purge all stats up-to a given instant
   *
   * @param timestamp   - exclusive
   */
  boolean purgeUpTo(Instant timestamp);

  /**
   * Get a "timeline" of instance history usage.
   *
   * @param from - inclusive
   * @param to   - exclusive
   */
  InstanceTimeline aggregate(String accountId, long from, long to);

  List<InstanceStatsSnapshot> aggregate(String accountId, Instant from, Instant to);

  /**
   * Get the last time stats were saved for this account
   */
  @Nullable Instant getLastSnapshotTime(String accountId);

  /**
   * Get the last time stats were saved for this account
   */
  @Nullable Instant getFirstSnapshotTime(String accountId);

  /**
   * Calculate percentile of instance usage values
   *
   * @param p percentile number. eg 95 for 95th percentile
   * @return percentile over given time period
   */
  double percentile(String accountId, Instant from, Instant to, double p);
}
