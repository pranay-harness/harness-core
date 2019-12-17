package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLBillingTimeDataPoint {
  QLReference key;
  Number value;
  Number max;
  Number avg;
  long time;

  public QLBillingDataPoint getQLBillingDataPoint() {
    return QLBillingDataPoint.builder().value(value).max(max).avg(avg).key(key).build();
  }
}
