package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLTimeFilter implements Filter {
  private QLTimeOperator operator;
  private Number value;

  @Override
  public Number[] getValues() {
    return new Number[] {value};
  }
}
