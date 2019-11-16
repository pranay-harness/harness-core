package io.harness.ccm.budget.entities;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import software.wings.beans.Environment.EnvironmentType;

@Data
@Builder
@JsonTypeName("APPLICATION")
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ApplicationBudgetScopeKeys")
public class ApplicationBudgetScope implements BudgetScope {
  String[] applicationIds;
  EnvironmentType type;
}
