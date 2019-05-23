package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class QLStackedDataPoint {
  QLReference key;
  List<QLDataPoint> values;
}
