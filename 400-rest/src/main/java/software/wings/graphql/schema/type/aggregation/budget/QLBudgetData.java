package software.wings.graphql.schema.type.aggregation.budget;

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
public class QLBudgetData {
  long time;
  Double actualCost;
  Double budgeted;
  Double budgetVariance;
  Double budgetVariancePercentage;
}
