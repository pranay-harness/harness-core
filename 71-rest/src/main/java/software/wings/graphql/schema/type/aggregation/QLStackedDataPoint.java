package software.wings.graphql.schema.type.aggregation;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QLStackedDataPoint {
  QLReference key;
  List<QLDataPoint> values;
}
