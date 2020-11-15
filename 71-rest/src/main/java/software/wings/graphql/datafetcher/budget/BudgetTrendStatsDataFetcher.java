package software.wings.graphql.datafetcher.budget;

import com.google.inject.Inject;

import io.harness.ccm.budget.BudgetService;
import io.harness.ccm.budget.entities.Budget;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.datafetcher.billing.BillingDataHelper;
import software.wings.graphql.schema.query.QLBudgetQueryParameters;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingStatsInfo;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTrendStats;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ce.CeAccountExpirationChecker;

@Slf4j
public class BudgetTrendStatsDataFetcher
    extends AbstractObjectDataFetcher<QLBudgetTrendStats, QLBudgetQueryParameters> {
  public static final String BUDGET_DOES_NOT_EXIST_MSG = "Budget does not exist";

  private static final String ACTUAL_COST_LABEL = "Actual vs. budgeted";
  private static final String COST_VALUE = "$%s / $%s";
  private static final String FORECASTED_COST_LABEL = "Forecasted vs. budgeted";
  private static final String EMPTY_VALUE = "-";

  @Inject BudgetService budgetService;
  @Inject BillingDataHelper billingDataHelper;
  @Inject CeAccountExpirationChecker accountChecker;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLBudgetTrendStats fetch(QLBudgetQueryParameters parameters, String accountId) {
    accountChecker.checkIsCeEnabled(accountId);
    Budget budget = null;
    if (parameters.getBudgetId() != null) {
      log.info("Fetching budgetTrendStats data");
      budget = budgetService.get(parameters.getBudgetId(), accountId);
    }
    if (budget == null) {
      throw new InvalidRequestException(BUDGET_DOES_NOT_EXIST_MSG, WingsException.USER);
    }
    Double actualCost = budgetService.getActualCost(budget);
    double forecastCostOffset = budgetService.isStartOfMonth() ? 0.0 : actualCost;

    return QLBudgetTrendStats.builder()
        .totalCost(getCostStats(ACTUAL_COST_LABEL, actualCost, budget.getBudgetAmount()))
        .forecastCost(getCostStats(FORECASTED_COST_LABEL, forecastCostOffset + budgetService.getForecastCost(budget),
            budget.getBudgetAmount()))
        .budgetDetails(budgetService.getBudgetDetails(budget))
        .build();
  }

  private QLBillingStatsInfo getCostStats(String label, Double costValue, Double budgetedValue) {
    String statsValue = String.format(COST_VALUE, billingDataHelper.getRoundedDoubleValue(costValue),
        billingDataHelper.getRoundedDoubleValue(budgetedValue));
    return QLBillingStatsInfo.builder().statsLabel(label).statsDescription(EMPTY_VALUE).statsValue(statsValue).build();
  }
}
