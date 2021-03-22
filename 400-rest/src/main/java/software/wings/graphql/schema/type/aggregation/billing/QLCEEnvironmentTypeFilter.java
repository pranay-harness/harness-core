package software.wings.graphql.schema.type.aggregation.billing;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
