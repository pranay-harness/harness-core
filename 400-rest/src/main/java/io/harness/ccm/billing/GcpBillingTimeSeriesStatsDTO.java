package io.harness.ccm.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLData;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.experimental.FieldDefaults;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class GcpBillingTimeSeriesStatsDTO implements QLData {
  List<TimeSeriesDataPoints> stats;
}
