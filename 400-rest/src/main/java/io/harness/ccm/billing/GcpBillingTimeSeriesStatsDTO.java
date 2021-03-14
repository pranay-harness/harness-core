package io.harness.ccm.billing;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLData;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.experimental.FieldDefaults;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(Module._490_CE_COMMONS)
public class GcpBillingTimeSeriesStatsDTO implements QLData {
  List<TimeSeriesDataPoints> stats;
}
