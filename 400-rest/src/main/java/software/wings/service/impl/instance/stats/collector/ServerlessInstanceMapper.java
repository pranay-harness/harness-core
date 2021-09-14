/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.instance.stats.collector;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.stream.Collectors.groupingBy;

import io.harness.data.structure.EmptyPredicate;

import software.wings.beans.EntityType;
import software.wings.beans.infrastructure.instance.InvocationCount;
import software.wings.beans.infrastructure.instance.InvocationCount.InvocationCountKey;
import software.wings.beans.infrastructure.instance.ServerlessInstance;
import software.wings.beans.infrastructure.instance.stats.Mapper;
import software.wings.beans.infrastructure.instance.stats.ServerlessInstanceStats;
import software.wings.beans.infrastructure.instance.stats.ServerlessInstanceStats.AggregateInvocationCount;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class ServerlessInstanceMapper implements Mapper<Collection<ServerlessInstance>, ServerlessInstanceStats> {
  private Instant instant;
  private String accountId;

  @Override
  public ServerlessInstanceStats map(Collection<ServerlessInstance> instances) {
    final Collection<AggregateInvocationCount> appCounts = aggregateByAppInvocationCountKey(instances);
    return new ServerlessInstanceStats(instant, accountId, appCounts);
  }

  private Collection<AggregateInvocationCount> aggregateByAppInvocationCountKey(
      Collection<ServerlessInstance> instances) {
    return instances.stream()
        .collect(groupingBy(ServerlessInstance::getAppId))
        .values()
        .stream()
        .flatMap(this::processInstancesWithSameAppId)
        .collect(Collectors.toList());
  }
  // all instances should belong to same app id
  private Stream<AggregateInvocationCount> processInstancesWithSameAppId(Collection<ServerlessInstance> instances) {
    Optional<ServerlessInstance> firstServiceInstance = instances.stream().findFirst();
    if (firstServiceInstance.isPresent()) {
      final String appName = firstServiceInstance.get().getAppName();
      final String appId = firstServiceInstance.get().getAppId();
      return instances.stream()
          .filter(instance -> instance.getInstanceInfo() != null)
          .map(ServerlessInstance::getInstanceInfo)
          .filter(instanceInfo -> isNotEmpty(instanceInfo.getInvocationCountMap()))
          .flatMap(serverlessInstanceInfo -> serverlessInstanceInfo.getInvocationCountMap().values().stream())
          .collect(groupingBy(InvocationCount::getKey))
          .values()
          .stream()
          .filter(EmptyPredicate::isNotEmpty)
          .map(invocationCounts -> processInvocationCountsWithSameCountKey(invocationCounts, appId, appName));
    }
    return Stream.empty();
  }

  private AggregateInvocationCount processInvocationCountsWithSameCountKey(
      Collection<InvocationCount> invocationCounts, String appId, String appName) {
    final InvocationCountKey invocationCountKey = invocationCounts.iterator().next().getKey();
    final long count = invocationCounts.stream().mapToLong(InvocationCount::getCount).sum();
    return AggregateInvocationCount.builder()
        .entityType(EntityType.APPLICATION)
        .name(appName)
        .id(appId)
        .invocationCount(count)
        .invocationCountKey(invocationCountKey)
        .build();
  }
}
