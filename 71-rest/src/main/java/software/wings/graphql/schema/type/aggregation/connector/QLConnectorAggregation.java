package software.wings.graphql.schema.type.aggregation.connector;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Aggregation;
import software.wings.graphql.schema.type.aggregation.tag.QLTagAggregation;

@Value
@Builder
public class QLConnectorAggregation implements Aggregation {
  private QLConnectorTypeAggregation typeAggregation;
  private QLTagAggregation tagAggregation;
}