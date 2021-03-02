package software.wings.graphql.datafetcher.ce.exportData.dto;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.billing.QLTimeGroupType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._375_CE_GRAPHQL)
public class QLCETimeAggregation {
  QLTimeGroupType timePeriod;
}
