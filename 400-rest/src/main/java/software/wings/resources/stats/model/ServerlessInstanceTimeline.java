/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.stats.model;

import static java.util.stream.Collectors.toList;

import software.wings.beans.EntityType;
import software.wings.beans.infrastructure.instance.InvocationCount.InvocationCountKey;
import software.wings.beans.infrastructure.instance.stats.ServerlessInstanceStats;
import software.wings.beans.infrastructure.instance.stats.ServerlessInstanceStats.AggregateInvocationCount;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@AllArgsConstructor
public class ServerlessInstanceTimeline {
  @Value
  @AllArgsConstructor
  public static class DataPoint {
    private long timestamp;
    private String accountId;
    private List<Aggregate> aggregateInvocationCountList;
    private long totalInvocationCount;
    private InvocationCountKey invocationCountKey;
  }

  private List<DataPoint> points;

  public static ServerlessInstanceTimeline create(List<ServerlessInstanceStats> snapshots, Set<String> deletedAppIds,
      InvocationCountKey requiredInvocationCountKey) {
    return new ServerlessInstanceTimeline(snapshots.stream()
                                              .map(it -> markDeleted(it, deletedAppIds, requiredInvocationCountKey))
                                              .collect(Collectors.toList()));
  }

  private static DataPoint markDeleted(
      ServerlessInstanceStats snapshot, Set<String> deletedAppIds, InvocationCountKey requiredInvocationCountKey) {
    List<Aggregate> marked = snapshot.getAggregateCounts()
                                 .stream()
                                 .filter(ac -> ac.getEntityType() == EntityType.APPLICATION)
                                 .filter(ac -> ac.getInvocationCountKey() == requiredInvocationCountKey)
                                 .map(it -> new Aggregate(it, deletedAppIds.contains(it.getId()))) // mark deleted
                                 .collect(toList());
    return new DataPoint(snapshot.getTimestamp().toEpochMilli(), snapshot.getAccountId(), marked,
        marked.stream().mapToLong(Aggregate::getInvocationCount).sum(), requiredInvocationCountKey);
  }

  // limits the aggregate counts to top N. This information is used in hover.
  public static ServerlessInstanceTimeline copyWithLimit(ServerlessInstanceTimeline timeline, int limit) {
    List<DataPoint> filteredPoints = new LinkedList<>();
    for (DataPoint point : timeline.points) {
      List<Aggregate> topAggregates = point.aggregateInvocationCountList.stream()
                                          .sorted(Comparator.comparingLong(Aggregate::getInvocationCount).reversed())
                                          .limit(limit)
                                          .collect(toList());

      DataPoint p = new DataPoint(point.getTimestamp(), point.getAccountId(), topAggregates,
          point.getTotalInvocationCount(), point.getInvocationCountKey());
      filteredPoints.add(p);
    }

    return new ServerlessInstanceTimeline(filteredPoints);
  }

  @Value
  @AllArgsConstructor
  public static class Aggregate {
    private String entityType;
    private String name;
    private String id;
    @NonFinal private boolean entityDeleted;
    @NonFinal private long invocationCount;
    private InvocationCountKey invocationCountKey;

    public Aggregate(@NotNull AggregateInvocationCount aggregateCount, boolean isEntityDeleted) {
      this.entityType = aggregateCount.getEntityType().name();
      this.name = aggregateCount.getName();
      this.id = aggregateCount.getId();
      this.invocationCount = aggregateCount.getInvocationCount();
      this.entityDeleted = isEntityDeleted;
      invocationCountKey = aggregateCount.getInvocationCountKey();
    }
  }
}
