package io.harness.ccm.budget.entities;

import static io.harness.ccm.budget.entities.BudgetScopeType.APPLICATION;
import static io.harness.ccm.budget.entities.BudgetScopeType.CLUSTER;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = ApplicationBudgetScope.class, name = APPLICATION)
  , @JsonSubTypes.Type(value = ClusterBudgetScope.class, name = CLUSTER)
})
public interface BudgetScope {
  QLBillingDataFilter getBudgetScopeFilter();
}
