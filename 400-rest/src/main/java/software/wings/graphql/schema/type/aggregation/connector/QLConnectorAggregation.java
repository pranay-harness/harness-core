package software.wings.graphql.schema.type.aggregation.connector;

import software.wings.graphql.schema.type.aggregation.Aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLConnectorAggregation implements Aggregation {
  private QLConnectorTypeAggregation typeAggregation;
}
