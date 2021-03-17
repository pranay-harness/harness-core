package software.wings.graphql.schema.type.aggregation;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLTimeFilter implements Filter {
  private QLTimeOperator operator;
  private Number value;

  @Override
  public Number[] getValues() {
    return new Number[] {value};
  }
}
