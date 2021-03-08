package software.wings.graphql.schema.type.aggregation.budget;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

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
@TargetModule(Module._380_CG_GRAPHQL)
public class QLBudgetTrendStats implements QLData {
  QLBillingStatsInfo totalCost;
  QLBillingStatsInfo forecastCost;
  QLBudgetTableData budgetDetails;
  String status;
}
