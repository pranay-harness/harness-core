package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLStringFilter implements Filter {
  private QLStringOperator operator;
  private String[] values;
}
