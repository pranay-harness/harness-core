/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.type.aggregation.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
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
public class QLBillingDataFilter implements EntityFilter {
  private QLIdFilter application;
  private QLIdFilter service;
  private QLIdFilter environment;
  private QLIdFilter cluster;
  private QLIdFilter cloudServiceName;
  private QLIdFilter launchType;
  private QLIdFilter taskId;
  private QLIdFilter instanceType;
  private QLIdFilter instanceName;
  private QLIdFilter namespace;
  private QLIdFilter workloadName;
  private QLIdFilter workloadType;
  private QLIdFilter cloudProvider;
  private QLIdFilter nodeInstanceId;
  private QLIdFilter podInstanceId;
  private QLIdFilter parentInstanceId;
  private QLIdFilter labelSearch;
  private QLIdFilter tagSearch;
  private QLTimeFilter startTime;
  private QLTimeFilter endTime;
  private QLBillingDataTagFilter tag;
  private QLBillingDataLabelFilter label;
  private QLCEEnvironmentTypeFilter envType;
  // For budget alerts
  private QLTimeFilter alertTime;
  private QLIdFilter view;

  private QLNumberFilter storageUtilizationValue;

  public static Set<QLBillingDataFilterType> getFilterTypes(QLBillingDataFilter filter) {
    Set<QLBillingDataFilterType> filterTypes = new HashSet<>();
    if (filter.getApplication() != null) {
      filterTypes.add(QLBillingDataFilterType.Application);
    }
    if (filter.getStartTime() != null) {
      filterTypes.add(QLBillingDataFilterType.StartTime);
    }
    if (filter.getEndTime() != null) {
      filterTypes.add(QLBillingDataFilterType.EndTime);
    }
    if (filter.getCluster() != null) {
      filterTypes.add(QLBillingDataFilterType.Cluster);
    }
    if (filter.getService() != null) {
      filterTypes.add(QLBillingDataFilterType.Service);
    }
    if (filter.getEnvironment() != null) {
      filterTypes.add(QLBillingDataFilterType.Environment);
    }
    if (filter.getCloudServiceName() != null) {
      filterTypes.add(QLBillingDataFilterType.CloudServiceName);
    }
    if (filter.getLaunchType() != null) {
      filterTypes.add(QLBillingDataFilterType.LaunchType);
    }
    if (filter.getTaskId() != null) {
      filterTypes.add(QLBillingDataFilterType.TaskId);
    }
    if (filter.getInstanceType() != null) {
      filterTypes.add(QLBillingDataFilterType.InstanceType);
    }
    if (filter.getInstanceName() != null) {
      filterTypes.add(QLBillingDataFilterType.InstanceName);
    }
    if (filter.getNamespace() != null) {
      filterTypes.add(QLBillingDataFilterType.Namespace);
    }
    if (filter.getWorkloadName() != null) {
      filterTypes.add(QLBillingDataFilterType.WorkloadName);
    }
    if (filter.getWorkloadType() != null) {
      filterTypes.add(QLBillingDataFilterType.WorkloadType);
    }
    if (filter.getTag() != null) {
      filterTypes.add(QLBillingDataFilterType.Tag);
    }
    if (filter.getCloudProvider() != null) {
      filterTypes.add(QLBillingDataFilterType.CloudProvider);
    }
    if (filter.getLabel() != null) {
      filterTypes.add(QLBillingDataFilterType.Label);
    }
    if (filter.getNodeInstanceId() != null) {
      filterTypes.add(QLBillingDataFilterType.NodeInstanceId);
    }
    if (filter.getPodInstanceId() != null) {
      filterTypes.add(QLBillingDataFilterType.PodInstanceId);
    }
    if (filter.getParentInstanceId() != null) {
      filterTypes.add(QLBillingDataFilterType.ParentInstanceId);
    }
    if (filter.getEnvType() != null) {
      filterTypes.add(QLBillingDataFilterType.EnvironmentType);
    }
    if (filter.getLabelSearch() != null) {
      filterTypes.add(QLBillingDataFilterType.LabelSearch);
    }
    if (filter.getTagSearch() != null) {
      filterTypes.add(QLBillingDataFilterType.TagSearch);
    }
    if (filter.getAlertTime() != null) {
      filterTypes.add(QLBillingDataFilterType.AlertTime);
    }
    if (filter.getView() != null) {
      filterTypes.add(QLBillingDataFilterType.View);
    }
    if (filter.getStorageUtilizationValue() != null) {
      filterTypes.add(QLBillingDataFilterType.StorageUtilizationValue);
    }
    return filterTypes;
  }

  public static Filter getFilter(QLBillingDataFilterType type, QLBillingDataFilter filter) {
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
      case CloudServiceName:
        return filter.getCloudServiceName();
      case LaunchType:
        return filter.getLaunchType();
      case TaskId:
        return filter.getTaskId();
      case InstanceType:
        return filter.getInstanceType();
      case Namespace:
        return filter.getNamespace();
      case WorkloadName:
        return filter.getWorkloadName();
      case WorkloadType:
        return filter.getWorkloadType();
      case Tag:
        return filter.getTag();
      case CloudProvider:
        return filter.getCloudProvider();
      case Label:
        return filter.getLabel();
      case NodeInstanceId:
        return filter.getNodeInstanceId();
      case PodInstanceId:
        return filter.getPodInstanceId();
      case ParentInstanceId:
        return filter.getParentInstanceId();
      case EnvironmentType:
        return filter.getEnvType();
      case LabelSearch:
        return filter.getLabelSearch();
      case TagSearch:
        return filter.getTagSearch();
      case AlertTime:
        return filter.getAlertTime();
      case InstanceName:
        return filter.getInstanceName();
      case View:
        return filter.getView();
      case StorageUtilizationValue:
        return filter.getStorageUtilizationValue();
      default:
        throw new InvalidRequestException("Unsupported type " + type);
    }
  }
}
