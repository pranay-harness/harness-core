package io.harness.ccm.billing;

import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TimeSeriesDataPoints {
  List<QLBillingDataPoint> values;
  Long time;
}
