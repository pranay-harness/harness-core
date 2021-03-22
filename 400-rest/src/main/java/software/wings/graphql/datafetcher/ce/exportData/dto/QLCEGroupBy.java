package software.wings.graphql.datafetcher.ce.exportData.dto;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLCEGroupBy {
  QLCEEntityGroupBy entity;
  QLCETimeAggregation time;
  QLCETagAggregation tagAggregation;
  QLCELabelAggregation labelAggregation;
}
