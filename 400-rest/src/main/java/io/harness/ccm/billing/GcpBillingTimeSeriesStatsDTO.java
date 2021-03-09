package io.harness.ccm.billing;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import software.wings.graphql.schema.type.aggregation.QLData;

import java.util.List;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GcpBillingTimeSeriesStatsDTO implements QLData {
  List<TimeSeriesDataPoints> stats;
}
