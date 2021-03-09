package io.harness.ccm.billing.preaggregated;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingStatsInfo;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PreAggregateBillingTrendStatsDTO implements QLData {
  QLBillingStatsInfo unBlendedCost;
  QLBillingStatsInfo blendedCost;
  QLBillingStatsInfo cost;
}
