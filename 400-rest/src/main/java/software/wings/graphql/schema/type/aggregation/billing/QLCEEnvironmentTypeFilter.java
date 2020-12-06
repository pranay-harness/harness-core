package software.wings.graphql.schema.type.aggregation.billing;

import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;

public class QLCEEnvironmentTypeFilter implements Filter {
  private QLEnvType type;

  @Override
  public QLIdOperator getOperator() {
    return QLIdOperator.EQUALS;
  }

  @Override
  public Object[] getValues() {
    if (type != null) {
      return new String[] {type.toString()};
    }
    return null;
  }
}
