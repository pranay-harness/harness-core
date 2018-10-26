package software.wings.resources.stats.model;

import static java.util.stream.Collectors.toList;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import software.wings.beans.EntityType;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot.AggregateCount;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InstanceTimeline {
  @Value
  @AllArgsConstructor
  public static class DataPoint {
    private long timestamp;
    private String accountId;
    private List<AggregateCount> aggregateCounts;
    private int total;
  }

  public static InstanceTimeline from(List<InstanceStatsSnapshot> snapshots) {
    List<DataPoint> dataPoints = snapshots.stream().map(InstanceTimeline::mapToDataPoint).collect(Collectors.toList());
    return new InstanceTimeline(dataPoints);
  }

  private List<DataPoint> points;

  private static DataPoint mapToDataPoint(InstanceStatsSnapshot snapshot) {
    List<AggregateCount> filteredAndSortedCounts =
        snapshot.getAggregateCounts()
            .stream()
            .filter(ac -> ac.getEntityType() == EntityType.APPLICATION)
            .sorted(Comparator.comparingInt(AggregateCount::getCount).reversed())
            .limit(5)
            .collect(toList());

    return new DataPoint(
        snapshot.getTimestamp().toEpochMilli(), snapshot.getAccountId(), filteredAndSortedCounts, snapshot.getTotal());
  }
}
