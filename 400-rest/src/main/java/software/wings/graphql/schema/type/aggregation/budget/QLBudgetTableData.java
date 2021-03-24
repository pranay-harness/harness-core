package software.wings.graphql.schema.type.aggregation.budget;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

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
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLBudgetTableData {
  String name;
  String id;
  String type;
  String scopeType;
  String[] appliesTo;
  String[] appliesToIds;
  String environment;
  Double[] alertAt;
  String[] notifications;
  Double budgetedAmount;
  Double actualAmount;
  Double forecastCost;
  Double lastMonthCost;
  Long lastUpdatedAt;
}
