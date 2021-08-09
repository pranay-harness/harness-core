package software.wings.graphql.schema.type.aggregation;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface QLNumberFilterType extends QLFilterType {
  QLNumberFilter getNumberFilter();

  @Override
  default QLDataType getDataType() {
    return QLDataType.NUMBER;
  }
}
