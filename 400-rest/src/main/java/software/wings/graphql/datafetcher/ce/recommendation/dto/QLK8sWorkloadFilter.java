package software.wings.graphql.datafetcher.ce.recommendation.dto;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._375_CE_GRAPHQL)
public class QLK8sWorkloadFilter {
  QLIdFilter cluster;
  QLIdFilter namespace;
  QLIdFilter workloadName;
  QLIdFilter workloadType;
  QLTimeFilter startDate;
  QLTimeFilter endDate;
}
