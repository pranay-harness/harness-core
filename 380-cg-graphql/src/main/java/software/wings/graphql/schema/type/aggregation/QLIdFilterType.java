package software.wings.graphql.schema.type.aggregation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDP)
public interface QLIdFilterType extends QLFilterType {
  QLIdFilter getIdFilter();

  @Override
  default QLDataType getDataType() {
    return QLDataType.ID;
  }
}