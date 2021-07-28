package io.harness.ccm.commons.entities.budget;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BudgetData {
  List<BudgetCostData> costData;
  double forecastCost;
}
