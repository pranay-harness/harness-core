package io.harness.ccm.budget.entities;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ClusterBudgetScopeKeys")
public class ClusterBudgetScope implements BudgetScope {
  String[] clusterIds;
}
