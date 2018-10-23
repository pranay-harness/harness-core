package software.wings.resources.stats.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot.AggregateCount;

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
    List<DataPoint> dataPoints =
        snapshots.stream()
            .map(snapshot
                -> new DataPoint(snapshot.getTimestamp().toEpochMilli(), snapshot.getAccountId(),
                    snapshot.getAggregateCounts(), snapshot.getTotal()))
            .collect(Collectors.toList());

    return new InstanceTimeline(dataPoints);
  }

  private List<DataPoint> points;
}
