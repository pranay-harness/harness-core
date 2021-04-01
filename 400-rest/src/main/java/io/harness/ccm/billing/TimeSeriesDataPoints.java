package io.harness.ccm.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
public class TimeSeriesDataPoints {
  List<QLBillingDataPoint> values;
  Long time;
}
