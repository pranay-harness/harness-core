package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLNumberFilter implements Filter {
  private QLNumberOperator operator;
  private Number[] values;
}
