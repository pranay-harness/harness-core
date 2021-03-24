package software.wings.graphql.schema.type.aggregation.cloudprovider;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;

import lombok.Builder;
import lombok.Value;

/**
 * @author adarsh on 03/19/21
 */
@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLCEEnabledFilter implements Filter {
  private QLEnumOperator operator;
  private Boolean[] values;
}