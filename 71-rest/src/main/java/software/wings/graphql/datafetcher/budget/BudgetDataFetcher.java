package software.wings.graphql.datafetcher.budget;

import com.google.inject.Inject;

import io.harness.ccm.budget.BudgetService;
import io.harness.ccm.budget.entities.Budget;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.datafetcher.billing.QLBillingStatsHelper;
import software.wings.graphql.schema.query.QLBudgetQueryParameters;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetDataList;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

@Slf4j
public class BudgetDataFetcher extends AbstractObjectDataFetcher<QLBudgetDataList, QLBudgetQueryParameters> {
  public static final String BUDGET_DOES_NOT_EXIST_MSG = "Budget does not exist";

  @Inject BudgetService budgetService;
  @Inject QLBillingStatsHelper statsHelper;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLBudgetDataList fetch(QLBudgetQueryParameters qlQuery, String accountId) {
    Budget budget = null;
    if (qlQuery.getBudgetId() != null) {
      logger.info("Fetching budget data");
      budget = budgetService.get(qlQuery.getBudgetId());
    }
    if (budget == null) {
      throw new InvalidRequestException(BUDGET_DOES_NOT_EXIST_MSG, WingsException.USER);
    }
    return budgetService.getBudgetData(budget);
  }
}