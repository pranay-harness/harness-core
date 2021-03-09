package io.harness.ccm.billing;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint;

import java.util.List;

@Value
@Builder
public class TimeSeriesDataPoints {
  List<QLBillingDataPoint> values;
  Long time;
}
