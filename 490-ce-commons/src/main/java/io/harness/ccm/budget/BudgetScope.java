package io.harness.ccm.budget;

import static io.harness.ccm.budget.BudgetScopeType.APPLICATION;
import static io.harness.ccm.budget.BudgetScopeType.CLUSTER;
import static io.harness.ccm.budget.BudgetScopeType.PERSPECTIVE;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = ApplicationBudgetScope.class, name = APPLICATION)
  , @JsonSubTypes.Type(value = ClusterBudgetScope.class, name = CLUSTER),
      @JsonSubTypes.Type(value = PerspectiveBudgetScope.class, name = PERSPECTIVE)
})
@Schema(description = "The scope in which the Budget was created")
public interface BudgetScope {
  String getBudgetScopeType();
  List<String> getEntityIds();
  List<String> getEntityNames();
}