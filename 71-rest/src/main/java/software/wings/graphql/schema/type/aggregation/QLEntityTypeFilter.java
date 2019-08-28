package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLEntityTypeFilter implements Filter {
  private QLEnumOperator operator;
  private QLEntityType[] values;
}
