package software.wings.graphql.schema.type.aggregation.billing;

import io.harness.exception.InvalidRequestException;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

import java.util.HashSet;
import java.util.Set;

@Value
@Builder
@ToString
public class QLBillingDataFilter implements EntityFilter {
  private QLIdFilter application;
  private QLIdFilter service;
  private QLIdFilter environment;
  private QLIdFilter cluster;
  private QLTimeFilter endTime;
  private QLTimeFilter startTime;

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
      default:
        throw new InvalidRequestException("Unsupported type " + type);
    }
  }
}
