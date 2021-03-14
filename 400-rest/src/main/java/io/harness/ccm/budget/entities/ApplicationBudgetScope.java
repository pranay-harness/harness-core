package io.harness.ccm.budget.entities;

import static io.harness.ccm.budget.BudgetScopeType.APPLICATION;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@JsonTypeName("APPLICATION")
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ApplicationBudgetScopeKeys")
@TargetModule(Module._490_CE_COMMONS)
public class ApplicationBudgetScope implements BudgetScope {
  String[] applicationIds;
  EnvironmentType environmentType;

  @Override
  public String getBudgetScopeType() {
    return APPLICATION;
  }

  @Override
  public List<String> getEntityIds() {
    return Arrays.asList(applicationIds);
  }

  @Override
  public List<String> getEntityNames() {
    return null;
  }
}
