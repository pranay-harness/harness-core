package io.harness.ccm.billing.preaggregated;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingStatsInfo;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class PreAggregateBillingTrendStatsDTO implements QLData {
  QLBillingStatsInfo unBlendedCost;
  QLBillingStatsInfo blendedCost;
  QLBillingStatsInfo cost;
}
