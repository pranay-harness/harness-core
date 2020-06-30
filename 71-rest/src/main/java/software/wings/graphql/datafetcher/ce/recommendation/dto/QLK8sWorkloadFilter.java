package software.wings.graphql.datafetcher.ce.recommendation.dto;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

@Value
@Builder
public class QLK8sWorkloadFilter {
  QLIdFilter cluster;
  QLIdFilter namespace;
  QLIdFilter workloadName;
  QLWorkloadTypeFilter workloadType;
}
