package software.wings.graphql.schema.type.aggregation.billing;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Aggregation;

@Value
@Builder
public class QLCCMGroupBy implements Aggregation {
  private QLCCMEntityGroupBy entityGroupBy;
  private QLCCMTimeSeriesAggregation timeAggregation;
}