package software.wings.graphql.schema.type.aggregation.cloudprovider;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Aggregation;

@Value
@Builder
public class QLCloudProviderAggregation implements Aggregation {
  private QLCloudProviderTypeAggregation typeAggregation;
}
