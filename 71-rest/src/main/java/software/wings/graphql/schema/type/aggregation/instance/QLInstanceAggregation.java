package software.wings.graphql.schema.type.aggregation.instance;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Aggregation;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;

@Value
@Builder
public class QLInstanceAggregation implements Aggregation {
  private QLInstanceEntityAggregation entityAggregation;
  private QLTimeSeriesAggregation timeAggregation;
  private QLInstanceTagAggregation tagAggregation;
}
