package io.harness.ccm.budget.entities;

import static io.harness.ccm.budget.entities.BudgetScopeType.APPLICATION;
import static io.harness.ccm.budget.entities.BudgetScopeType.CLUSTER;
import static io.harness.ccm.budget.entities.BudgetScopeType.PERSPECTIVE;

import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = ApplicationBudgetScope.class, name = APPLICATION)
  , @JsonSubTypes.Type(value = ClusterBudgetScope.class, name = CLUSTER),
      @JsonSubTypes.Type(value = PerspectiveBudgetScope.class, name = PERSPECTIVE)
})
public interface BudgetScope {
  QLBillingDataFilter getBudgetScopeFilter();
  List<String> getEntityNames();
}
