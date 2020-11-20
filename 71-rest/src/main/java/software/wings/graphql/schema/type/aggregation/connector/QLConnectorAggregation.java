package software.wings.graphql.schema.type.aggregation.connector;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Aggregation;

@Value
@Builder
public class QLConnectorAggregation implements Aggregation {
  private QLConnectorTypeAggregation typeAggregation;
}
