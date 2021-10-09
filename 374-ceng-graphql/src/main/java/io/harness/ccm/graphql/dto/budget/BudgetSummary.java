package io.harness.ccm.graphql.dto.budget;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BudgetSummary {
  String id;
  String name;
  Double budgetAmount;
  Double actualCost;
  int timeLeft;
  String timeUnit;
  String timeScope;
  List<Double> actualCostAlerts;
  List<Double> forecastCostAlerts;
}
