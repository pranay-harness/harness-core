package software.wings.graphql.schema.type.aggregation;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDP)
public interface QLIdFilterType extends QLFilterType {
  QLIdFilter getIdFilter();

  @Override
  default QLDataType getDataType() {
    return QLDataType.ID;
  }
}