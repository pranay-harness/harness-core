package io.harness.cvng.servicelevelobjective.beans;

import java.util.List;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceLevelObjectiveFilter {
  @QueryParam("userJourneys") List<String> userJourneys;
  @QueryParam("identifiers") List<String> identifiers;
  @QueryParam("sliTypes") List<ServiceLevelIndicatorType> sliTypes;
  @QueryParam("targetTypes") List<SLOTargetType> targetTypes;
  @QueryParam("errorBudgetRisks") List<ErrorBudgetRisk> errorBudgetRisks;
}
