package io.harness.ccm.budget.entities;

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
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ClusterBudgetScopeKeys")
public class ClusterBudgetScope implements BudgetScope {
  String[] clusterIds;

  @Override
  public QLBillingDataFilter getBudgetFilter() {
    return QLBillingDataFilter.builder()
        .cluster(QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(clusterIds).build())
        .build();
  }
}
