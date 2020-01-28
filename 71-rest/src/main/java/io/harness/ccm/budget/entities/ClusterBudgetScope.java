package io.harness.ccm.budget.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;

@Data
@Builder
@JsonTypeName("CLUSTER")
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ClusterBudgetScopeKeys")
public class ClusterBudgetScope implements BudgetScope {
  String[] clusterIds;

  @Override
  public QLBillingDataFilter getBudgetScopeFilter() {
    return QLBillingDataFilter.builder()
        .cluster(QLIdFilter.builder().operator(QLIdOperator.IN).values(clusterIds).build())
        .build();
  }
}
