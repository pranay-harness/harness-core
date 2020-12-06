package software.wings.graphql.datafetcher.cloudefficiencyevents;

import software.wings.graphql.schema.type.aggregation.QLData;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLEventData implements QLData {
  List<QLEventsDataPoint> data;
  List<QLChartDataPoint> chartData;
}
