package software.wings.graphql.schema.type.aggregation.cloudprovider;

import software.wings.graphql.schema.type.aggregation.Aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCloudProviderAggregation implements Aggregation {
  private QLCloudProviderTypeAggregation typeAggregation;
}
