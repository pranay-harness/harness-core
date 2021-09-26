package software.wings.graphql.schema.type.aggregation;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

/**
 * This class is an intermediate class to build the stack data point data structure.
 */
@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDP)
public class QLIntermediateStackDataPoint {
  String groupBy1;
  QLReference key;
  Number value;

  public QLDataPoint getDataPoint() {
    return QLDataPoint.builder().key(key).value(value).build();
  }
}
