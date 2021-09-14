/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.instance.stats.collector;

import software.wings.beans.EntityType;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot.AggregateCount;
import software.wings.beans.infrastructure.instance.stats.Mapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class InstanceMapper implements Mapper<Collection<Instance>, InstanceStatsSnapshot> {
  private Instant instant;
  private String accountId;

  @Override
  public InstanceStatsSnapshot map(Collection<Instance> instances) {
    Collection<AggregateCount> appCounts = aggregateByApp(instances);
    List<AggregateCount> aggregateCounts = new ArrayList<>(appCounts);

    return new InstanceStatsSnapshot(instant, accountId, aggregateCounts);
  }

  private Collection<AggregateCount> aggregateByApp(Collection<Instance> instances) {
    // key = appId
    Map<String, AggregateCount> appCounts = new HashMap<>();

    for (final Instance instance : instances) {
      AggregateCount appCount = appCounts.computeIfAbsent(instance.getAppId(),
          appId -> new AggregateCount(EntityType.APPLICATION, instance.getAppName(), instance.getAppId(), 0));
      appCount.incrementCount(1);
    }

    return appCounts.values();
  }
}
