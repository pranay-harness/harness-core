package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QLPodCountDataPoint {
  QLReference key;
  Number value;
}
