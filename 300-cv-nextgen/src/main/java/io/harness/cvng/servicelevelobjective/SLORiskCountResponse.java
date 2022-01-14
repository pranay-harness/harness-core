package io.harness.cvng.servicelevelobjective;

import io.harness.cvng.servicelevelobjective.beans.ErrorBudgetRisk;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class SLORiskCountResponse {
  private Integer totalCount;
  @Singular private List<RiskCount> riskCounts;

  @Value
  @Builder
  public static class RiskCount {
    ErrorBudgetRisk errorBudgetRisk;
    Integer count;

    @JsonIgnore
    public ErrorBudgetRisk getErrorBudgetRisk() {
      return errorBudgetRisk;
    }

    public String getIdentifier() {
      return errorBudgetRisk.name();
    }

    public String getDisplayName() {
      return errorBudgetRisk.getDisplayName();
    }
  }
}
