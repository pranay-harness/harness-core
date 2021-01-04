package software.wings.graphql.schema.type.aggregation.budget;

import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingStatsInfo;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.BUDGET)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLBudgetTrendStats implements QLData {
  QLBillingStatsInfo totalCost;
  QLBillingStatsInfo forecastCost;
  QLBudgetTableData budgetDetails;
}
