package io.harness.ccm.budget.entities;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

import static io.harness.ccm.budget.BudgetScopeType.APPLICATION;
import static io.harness.ccm.budget.BudgetScopeType.CLUSTER;
import static io.harness.ccm.budget.BudgetScopeType.PERSPECTIVE;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = ApplicationBudgetScope.class, name = APPLICATION)
  , @JsonSubTypes.Type(value = ClusterBudgetScope.class, name = CLUSTER),
      @JsonSubTypes.Type(value = PerspectiveBudgetScope.class, name = PERSPECTIVE)
})
public interface BudgetScope {
  String getBudgetScopeType();
  List<String> getEntityIds();
  List<String> getEntityNames();
}