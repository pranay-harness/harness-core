package io.harness.ccm.billing.preaggregated;

import io.harness.ccm.billing.TimeSeriesDataPoints;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.graphql.schema.type.aggregation.QLData;

import java.util.List;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PreAggregateBillingTimeSeriesStatsDTO implements QLData {
  List<TimeSeriesDataPoints> stats;
}
