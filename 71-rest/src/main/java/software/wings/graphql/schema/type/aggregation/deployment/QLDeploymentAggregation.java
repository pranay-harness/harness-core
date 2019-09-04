package software.wings.graphql.schema.type.aggregation.deployment;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Aggregation;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.tag.QLTagAggregation;

@Value
@Builder
public class QLDeploymentAggregation implements Aggregation {
  private QLDeploymentEntityAggregation entityAggregation;
  private QLTimeSeriesAggregation timeAggregation;
  private QLTagAggregation tagAggregation;
}