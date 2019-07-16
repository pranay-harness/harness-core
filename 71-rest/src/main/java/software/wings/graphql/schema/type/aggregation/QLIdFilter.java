package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLIdFilter implements Filter {
  private QLIdOperator operator;
  private String[] values;
}
