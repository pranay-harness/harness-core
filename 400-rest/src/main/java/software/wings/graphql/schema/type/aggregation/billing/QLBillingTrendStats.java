package software.wings.graphql.schema.type.aggregation.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@Scope(ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class QLBillingTrendStats implements QLData {
  QLBillingStatsInfo totalCost;
  QLBillingStatsInfo costTrend;
  QLBillingStatsInfo forecastCost;
  QLBillingStatsInfo idleCost;
  QLBillingStatsInfo utilizedCost;
  QLBillingStatsInfo unallocatedCost;
  QLBillingStatsInfo systemCost;
  QLBillingStatsInfo efficiencyScore;
}
