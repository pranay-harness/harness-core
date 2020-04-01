package software.wings.graphql.schema.type.aggregation.billing;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLBillingTrendStats implements QLData {
  QLBillingStatsInfo totalCost;
  QLBillingStatsInfo costTrend;
  QLBillingStatsInfo forecastCost;
  QLBillingStatsInfo idleCost;
  QLBillingStatsInfo utilizedCost;
  QLBillingStatsInfo unallocatedCost;
}
