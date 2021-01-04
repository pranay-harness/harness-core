package software.wings.graphql.schema.type.aggregation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QLBillingStackedTimeSeriesDataPoint {
  List<QLBillingDataPoint> values;
  Long time;
}
