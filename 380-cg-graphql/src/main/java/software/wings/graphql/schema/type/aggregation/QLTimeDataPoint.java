package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLTimeDataPoint {
  QLReference key;
  Number value;
  long time;

  public QLDataPoint getQLDataPoint() {
    return QLDataPoint.builder().value(value).key(key).build();
  }
}
