package software.wings.graphql.schema.type.aggregation.environment;

import software.wings.graphql.schema.type.QLEnvironmentType;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;

import lombok.Builder;
import lombok.Value;

/**
 * @author rktummala on 07/18/19
 */
@Value
@Builder
public class QLEnvironmentTypeFilter implements Filter {
  private QLEnumOperator operator;
  private QLEnvironmentType[] values;
}
