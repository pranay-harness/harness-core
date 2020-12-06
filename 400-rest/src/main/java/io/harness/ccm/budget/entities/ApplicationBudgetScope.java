package io.harness.ccm.budget.entities;

import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
public class ApplicationBudgetScope implements BudgetScope {
  String[] applicationIds;
  EnvironmentType environmentType;

  @Override
  public QLBillingDataFilter getBudgetScopeFilter() {
    return QLBillingDataFilter.builder()
        .application(QLIdFilter.builder().operator(QLIdOperator.IN).values(applicationIds).build())
        .build();
  }
}
