package io.harness.ccm.billing;

import lombok.Builder;
import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint;

import java.util.List;

@Builder
public class TimeSeriesDataPoints {
  List<QLBillingDataPoint> values;
  Long time;
}
