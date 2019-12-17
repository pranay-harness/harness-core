package software.wings.graphql.schema.type.aggregation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QLBillingStackedTimeSeriesDataPoint {
  List<QLBillingDataPoint> values;
  Long time;
}