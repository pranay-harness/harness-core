package io.harness.ccm.billing;

import lombok.Builder;
import software.wings.graphql.schema.type.aggregation.QLData;

import java.util.List;

@Builder
public class GcpBillingTimeSeriesStatsDTO implements QLData {
  List<TimeSeriesDataPoints> stats;
}
