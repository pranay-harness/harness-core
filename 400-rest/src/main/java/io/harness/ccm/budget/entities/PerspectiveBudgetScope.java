package io.harness.ccm.budget.entities;

import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@JsonTypeName("PERSPECTIVE")
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "PerspectiveBudgetScopeKeys")
public class PerspectiveBudgetScope implements BudgetScope {
  String viewId;
  String viewName;

  @Override
  public QLBillingDataFilter getBudgetScopeFilter() {
    return QLBillingDataFilter.builder()
        .view(QLIdFilter.builder().operator(QLIdOperator.IN).values(new String[] {viewId}).build())
        .build();
  }

  @Override
  public List<String> getEntityNames() {
    return Collections.singletonList(viewName);
  }
}
