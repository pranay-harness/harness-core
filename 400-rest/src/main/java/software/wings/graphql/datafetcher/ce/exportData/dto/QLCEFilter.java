/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.ce.exportData.dto;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class QLCEFilter implements EntityFilter {
  private QLIdFilter application;
  private QLIdFilter service;
  private QLIdFilter environment;
  private QLIdFilter cluster;
  private QLIdFilter ecsService;
  private QLIdFilter launchType;
  private QLIdFilter task;
  private QLIdFilter instanceType;
  private QLIdFilter namespace;
  private QLIdFilter workload;
  private QLIdFilter node;
  private QLIdFilter pod;
  private QLTimeFilter startTime;
  private QLTimeFilter endTime;
  private QLCETagFilter tag;
  private QLCELabelFilter label;

  public static Set<QLCEFilterType> getFilterTypes(QLCEFilter filter) {
    Set<QLCEFilterType> filterTypes = new HashSet<>();
    if (filter.getApplication() != null) {
      filterTypes.add(QLCEFilterType.Application);
    }
    if (filter.getStartTime() != null) {
      filterTypes.add(QLCEFilterType.StartTime);
    }
    if (filter.getEndTime() != null) {
      filterTypes.add(QLCEFilterType.EndTime);
    }
    if (filter.getCluster() != null) {
      filterTypes.add(QLCEFilterType.Cluster);
    }
    if (filter.getService() != null) {
      filterTypes.add(QLCEFilterType.Service);
    }
    if (filter.getEnvironment() != null) {
      filterTypes.add(QLCEFilterType.Environment);
    }
    if (filter.getEcsService() != null) {
      filterTypes.add(QLCEFilterType.EcsService);
    }
    if (filter.getLaunchType() != null) {
      filterTypes.add(QLCEFilterType.LaunchType);
    }
    if (filter.getTask() != null) {
      filterTypes.add(QLCEFilterType.Task);
    }
    if (filter.getInstanceType() != null) {
      filterTypes.add(QLCEFilterType.InstanceType);
    }
    if (filter.getNamespace() != null) {
      filterTypes.add(QLCEFilterType.Namespace);
    }
    if (filter.getWorkload() != null) {
      filterTypes.add(QLCEFilterType.Workload);
    }
    if (filter.getNode() != null) {
      filterTypes.add(QLCEFilterType.Node);
    }
    if (filter.getPod() != null) {
      filterTypes.add(QLCEFilterType.Pod);
    }
    if (filter.getTag() != null) {
      filterTypes.add(QLCEFilterType.Tag);
    }
    if (filter.getLabel() != null) {
      filterTypes.add(QLCEFilterType.Label);
    }
    return filterTypes;
  }

  public static Filter getFilter(QLCEFilterType type, QLCEFilter filter) {
    switch (type) {
      case Application:
        return filter.getApplication();
      case Environment:
        return filter.getEnvironment();
      case Service:
        return filter.getService();
      case Cluster:
        return filter.getCluster();
      case EndTime:
        return filter.getEndTime();
      case StartTime:
        return filter.getStartTime();
      case EcsService:
        return filter.getEcsService();
      case LaunchType:
        return filter.getLaunchType();
      case Task:
        return filter.getTask();
      case InstanceType:
        return filter.getInstanceType();
      case Namespace:
        return filter.getNamespace();
      case Workload:
        return filter.getWorkload();
      case Node:
        return filter.getNode();
      case Pod:
        return filter.getPod();
      case Tag:
        return filter.getTag();
      case Label:
        return filter.getLabel();
      default:
        throw new InvalidRequestException("Unsupported type " + type);
    }
  }
}
