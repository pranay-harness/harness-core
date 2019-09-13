package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QLStackedDataPoint {
  QLReference key;
  List<QLDataPoint> values;
}
