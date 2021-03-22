package software.wings.graphql.schema.type.aggregation;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLBillingTimeDataPoint {
  QLReference key;
  Number value;
  long time;

  public QLBillingDataPoint getQLBillingDataPoint() {
    return QLBillingDataPoint.builder().value(value).key(key).build();
  }
}
