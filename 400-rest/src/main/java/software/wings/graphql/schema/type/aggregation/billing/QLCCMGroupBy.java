package software.wings.graphql.schema.type.aggregation.billing;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.Aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLCCMGroupBy implements Aggregation {
  private QLCCMEntityGroupBy entityGroupBy;
  private QLCCMTimeSeriesAggregation timeAggregation;
  private QLBillingDataTagAggregation tagAggregation;
  private QLBillingDataLabelAggregation labelAggregation;
}
