package io.harness.ccm.billing.preaggregated;

import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingStatsInfo;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PreAggregateBillingTrendStatsDTO implements QLData {
  QLBillingStatsInfo unBlendedCost;
  QLBillingStatsInfo blendedCost;
  QLBillingStatsInfo cost;
}
