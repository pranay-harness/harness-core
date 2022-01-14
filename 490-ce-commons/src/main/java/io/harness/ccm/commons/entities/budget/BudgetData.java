package io.harness.ccm.commons.entities.budget;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(name = "BudgetData", description = "This object contains the Cost Data associated with a Budget")
public class BudgetData {
  List<BudgetCostData> costData;
  double forecastCost;
}
