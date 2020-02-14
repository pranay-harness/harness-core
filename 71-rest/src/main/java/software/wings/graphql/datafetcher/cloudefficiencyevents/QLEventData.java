package software.wings.graphql.datafetcher.cloudefficiencyevents;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLData;

import java.util.List;

@Value
@Builder
public class QLEventData implements QLData {
  List<QLEventsDataPoint> data;
  List<QLChartDataPoint> chartData;
}
