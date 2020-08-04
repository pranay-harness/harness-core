package software.wings.graphql.datafetcher.cloudefficiencyevents;

import io.harness.exception.InvalidRequestException;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

import java.util.EnumSet;
import java.util.Set;

@Value
@Builder
@ToString
public class QLEventsDataFilter {
  private QLIdFilter application;
  private QLIdFilter service;
  private QLIdFilter environment;
  private QLIdFilter cluster;
  private QLIdFilter cloudServiceName;
  private QLIdFilter taskId;
  private QLIdFilter namespace;
  private QLIdFilter workloadName;
  private QLIdFilter workloadType;
  private QLIdFilter billingAmount;
  private QLTimeFilter startTime;
  private QLTimeFilter endTime;

  public static Set<QLEventsDataFilterType> getFilterTypes(QLEventsDataFilter filter) {
    EnumSet<QLEventsDataFilterType> filterTypes = EnumSet.noneOf(QLEventsDataFilterType.class);
    if (filter.getApplication() != null) {
      filterTypes.add(QLEventsDataFilterType.Application);
    }
    if (filter.getStartTime() != null) {
      filterTypes.add(QLEventsDataFilterType.StartTime);
    }
    if (filter.getEndTime() != null) {
      filterTypes.add(QLEventsDataFilterType.EndTime);
    }
    if (filter.getCluster() != null) {
      filterTypes.add(QLEventsDataFilterType.Cluster);
    }
    if (filter.getService() != null) {
      filterTypes.add(QLEventsDataFilterType.Service);
    }
    if (filter.getEnvironment() != null) {
      filterTypes.add(QLEventsDataFilterType.Environment);
    }
    if (filter.getCloudServiceName() != null) {
      filterTypes.add(QLEventsDataFilterType.CloudServiceName);
    }
    if (filter.getTaskId() != null) {
      filterTypes.add(QLEventsDataFilterType.TaskId);
    }
    if (filter.getNamespace() != null) {
      filterTypes.add(QLEventsDataFilterType.Namespace);
    }
    if (filter.getWorkloadName() != null) {
      filterTypes.add(QLEventsDataFilterType.WorkloadName);
    }
    if (filter.getWorkloadType() != null) {
      filterTypes.add(QLEventsDataFilterType.WorkloadType);
    }
    if (filter.getBillingAmount() != null) {
      filterTypes.add(QLEventsDataFilterType.BillingAmount);
    }
    return filterTypes;
  }

  public static Filter getFilter(QLEventsDataFilterType type, QLEventsDataFilter filter) {
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
      case TaskId:
        return filter.getTaskId();
      case Namespace:
        return filter.getNamespace();
      case WorkloadName:
        return filter.getWorkloadName();
      case WorkloadType:
        return filter.getWorkloadType();
      case BillingAmount:
        return filter.getBillingAmount();
      default:
        throw new InvalidRequestException("Unsupported type " + type);
    }
  }
}
