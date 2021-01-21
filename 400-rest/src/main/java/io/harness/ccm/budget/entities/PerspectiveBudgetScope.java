package io.harness.ccm.budget.entities;

import static io.harness.ccm.budget.BudgetScopeType.PERSPECTIVE;

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
  public String getBudgetScopeType() {
    return PERSPECTIVE;
  }

  @Override
  public List<String> getEntityIds() {
    return Collections.singletonList(viewId);
  }

  @Override
  public List<String> getEntityNames() {
    return Collections.singletonList(viewName);
  }
}
