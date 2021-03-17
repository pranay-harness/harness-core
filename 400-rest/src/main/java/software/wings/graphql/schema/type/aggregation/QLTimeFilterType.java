package software.wings.graphql.schema.type.aggregation;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._380_CG_GRAPHQL)
public interface QLTimeFilterType extends QLFilterType {
  QLTimeFilter getTimeFilter();

  @Override
  default QLDataType getDataType() {
    return QLDataType.TIME;
  }
}
