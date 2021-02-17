package software.wings.graphql.schema.type.aggregation.cloudprovider;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

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
@TargetModule(Module._380_CG_GRAPHQL)
public class QLCloudProviderTypeFilter implements Filter {
  private QLEnumOperator operator;
  private QLCloudProviderType[] values;
}
