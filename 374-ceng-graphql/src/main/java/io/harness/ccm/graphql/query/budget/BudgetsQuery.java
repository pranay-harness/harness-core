package io.harness.ccm.graphql.query.budget;

import static io.harness.ccm.budget.AlertThresholdBase.ACTUAL_COST;
import static io.harness.ccm.budget.AlertThresholdBase.FORECASTED_COST;

import io.harness.ccm.budget.dao.BudgetDao;
import io.harness.ccm.budget.utils.BudgetUtils;
import io.harness.ccm.commons.entities.billing.Budget;
import io.harness.ccm.graphql.dto.budget.BudgetSummary;
import io.harness.ccm.graphql.utils.GraphQLUtils;
import io.harness.ccm.graphql.utils.annotations.GraphQLApi;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@GraphQLApi
public class BudgetsQuery {
  @Inject private GraphQLUtils graphQLUtils;
  @Inject private BudgetDao budgetDao;

  @GraphQLQuery(name = "budgetSummary", description = "Budget card for perspectives")
  public BudgetSummary budgetSummaryForPerspective(@GraphQLArgument(name = "perspectiveId") String perspectiveId,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);
    try {
      List<Budget> budgets = budgetDao.list(accountId);
      List<Budget> perspectiveBudgets =
          budgets.stream()
              .filter(budget -> BudgetUtils.isBudgetBasedOnGivenPerspective(budget, perspectiveId))
              .collect(Collectors.toList());

      if (!perspectiveBudgets.isEmpty()) {
        // UI allows only one budget per perspective
        Budget perspectiveBudget = perspectiveBudgets.get(0);
        return BudgetSummary.builder()
            .id(perspectiveBudget.getUuid())
            .name(perspectiveBudget.getName())
            .budgetAmount(perspectiveBudget.getBudgetAmount())
            .actualCost(perspectiveBudget.getActualCost())
            .timeLeft(BudgetUtils.getTimeLeftForBudget(perspectiveBudget))
            .timeUnit(BudgetUtils.DEFAULT_TIME_UNIT)
            .timeScope(BudgetUtils.DEFAULT_TIME_SCOPE)
            .build();
      }

    } catch (Exception e) {
      log.info("Exception while fetching budget for given perspective: ", e);
    }
    return null;
  }

  @GraphQLQuery(name = "budgetList", description = "Budget List")
  public List<BudgetSummary> budgetList(@GraphQLArgument(name = "fetchOnlyPerspectiveBudgets",
                                            defaultValue = "false") boolean fetchOnlyPerspectiveBudgets,
      @GraphQLArgument(name = "limit", defaultValue = "1000") Integer limit,
      @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);
    List<BudgetSummary> budgetSummaryList = new ArrayList<>();
    List<Budget> budgets = budgetDao.list(accountId, limit, offset);
    if (fetchOnlyPerspectiveBudgets) {
      budgets = budgets.stream().filter(BudgetUtils::isPerspectiveBudget).collect(Collectors.toList());
    }
    budgets.forEach(budget
        -> budgetSummaryList.add(
            BudgetSummary.builder()
                .id(budget.getUuid())
                .name(budget.getName())
                .budgetAmount(budget.getBudgetAmount())
                .actualCost(budget.getActualCost())
                .forecastCost(budget.getForecastCost())
                .timeLeft(BudgetUtils.getTimeLeftForBudget(budget))
                .timeUnit(BudgetUtils.DEFAULT_TIME_UNIT)
                .timeScope(BudgetUtils.DEFAULT_TIME_SCOPE)
                .actualCostAlerts(BudgetUtils.getAlertThresholdsForBudget(budget, ACTUAL_COST))
                .forecastCostAlerts(BudgetUtils.getAlertThresholdsForBudget(budget, FORECASTED_COST))
                .build()));

    return budgetSummaryList;
  }
}
