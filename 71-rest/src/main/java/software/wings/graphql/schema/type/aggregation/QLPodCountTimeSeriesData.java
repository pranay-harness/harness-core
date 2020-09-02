package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QLPodCountTimeSeriesData implements QLData {
  private List<QLPodCountTimeSeriesDataPoint> data;
}
