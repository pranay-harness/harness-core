package io.harness.ccm.budget.entities;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import software.wings.beans.Environment.EnvironmentType;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ApplicationBudgetScopeKeys")
public class ApplicationBudgetScope implements BudgetScope {
  String[] applicationIds;
  EnvironmentType type;
}
