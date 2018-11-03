package software.wings.resources.stats.model;

import static java.util.stream.Collectors.toList;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;
import software.wings.beans.EntityType;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot.AggregateCount;
import software.wings.resources.DashboardStatisticsResource;
import software.wings.service.impl.instance.stats.collector.SimplePercentile;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Value
@AllArgsConstructor
public class InstanceTimeline {
  @Value
  @AllArgsConstructor
  public static class DataPoint {
    private long timestamp;
    private String accountId;
    private List<Aggregate> aggregateCounts;
    private int total;
  }

  private List<DataPoint> points;

  public InstanceTimeline(List<InstanceStatsSnapshot> snapshots, Set<String> deletedAppIds) {
    this.points = snapshots.stream().map(it -> mapToDataPoint(it, deletedAppIds)).collect(Collectors.toList());
  }

  private static DataPoint mapToDataPoint(InstanceStatsSnapshot snapshot, Set<String> deletedAppIds) {
    List<Aggregate> filteredAndSortedCounts = snapshot.getAggregateCounts()
                                                  .stream()
                                                  .filter(ac -> ac.getEntityType() == EntityType.APPLICATION)
                                                  .sorted(Comparator.comparingInt(AggregateCount::getCount).reversed())
                                                  .limit(5)
                                                  .map(it -> new Aggregate(it, deletedAppIds.contains(it.getId())))
                                                  .collect(toList());
    return new DataPoint(
        snapshot.getTimestamp().toEpochMilli(), snapshot.getAccountId(), filteredAndSortedCounts, snapshot.getTotal());
  }

  public Map<String, Object> getLocalPercentile() {
    Double p = DashboardStatisticsResource.DEFAULT_PERCENTILE;
    Integer percentileValue =
        new SimplePercentile(points.stream().map(DataPoint::getTotal).collect(toList())).evaluate(p);

    Map<String, Object> result = new HashMap<>();
    result.put("value", percentileValue);
    result.put("percentile", p);

    return result;
  }

  @Value
  @AllArgsConstructor
  public static class Aggregate {
    private EntityType entityType;
    private String name;
    private String id;
    @NonFinal private boolean entityDeleted;
    @NonFinal private int count;

    public Aggregate(AggregateCount aggregateCount, boolean isEntityDeleted) {
      this.entityType = aggregateCount.getEntityType();
      this.name = aggregateCount.getName();
      this.id = aggregateCount.getId();
      this.count = aggregateCount.getCount();
      this.entityDeleted = isEntityDeleted;
    }
  }
}
