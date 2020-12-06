package software.wings.graphql.schema.type.aggregation.billing;

import software.wings.graphql.schema.type.aggregation.Aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCCMGroupBy implements Aggregation {
  private QLCCMEntityGroupBy entityGroupBy;
  private QLCCMTimeSeriesAggregation timeAggregation;
  private QLBillingDataTagAggregation tagAggregation;
  private QLBillingDataLabelAggregation labelAggregation;
}
