package software.wings.graphql.schema.type.aggregation.connector;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLConnectorType;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;

import lombok.Builder;
import lombok.Value;

/**
 * @author rktummala on 07/18/19
 */
@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLConnectorTypeFilter implements Filter {
  private QLEnumOperator operator;
  private QLConnectorType[] values;
}
