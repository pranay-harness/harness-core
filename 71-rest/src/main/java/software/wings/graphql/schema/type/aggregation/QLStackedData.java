package software.wings.graphql.schema.type.aggregation;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QLStackedData implements QLData {
  List<QLStackedDataPoint> dataPoints;
}
