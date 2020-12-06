package software.wings.graphql.schema.type.aggregation.cloudprovider;

import software.wings.graphql.schema.type.QLCloudProviderType;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;

import lombok.Builder;
import lombok.Value;

/**
 * @author rktummala on 07/18/19
 */
@Value
@Builder
public class QLCloudProviderTypeFilter implements Filter {
  private QLEnumOperator operator;
  private QLCloudProviderType[] values;
}
