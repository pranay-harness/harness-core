package io.harness.ccm.billing;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._490_CE_COMMONS)
public class TimeSeriesDataPoints {
  List<QLBillingDataPoint> values;
  Long time;
}
