package software.wings.graphql.datafetcher.ce.recommendation.dto;

import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLK8sWorkloadFilter {
  QLIdFilter cluster;
  QLIdFilter namespace;
  QLIdFilter workloadName;
  QLIdFilter workloadType;
  QLTimeFilter startDate;
  QLTimeFilter endDate;
}
