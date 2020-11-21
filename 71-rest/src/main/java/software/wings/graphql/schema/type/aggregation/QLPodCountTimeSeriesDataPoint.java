package software.wings.graphql.schema.type.aggregation;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QLPodCountTimeSeriesDataPoint {
  private List<QLPodCountDataPoint> values;
  private Long time;
}
