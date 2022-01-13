/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.budget;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.budget.BudgetService;
import io.harness.ccm.commons.entities.billing.Budget;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.datafetcher.billing.BillingDataHelper;
import software.wings.graphql.schema.query.QLBudgetQueryParameters;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingStatsInfo;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTrendStats;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ce.CeAccountExpirationChecker;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class BudgetTrendStatsDataFetcher
    extends AbstractObjectDataFetcher<QLBudgetTrendStats, QLBudgetQueryParameters> {
  public static final String BUDGET_DOES_NOT_EXIST_MSG = "Budget does not exist";

  private static final String ACTUAL_COST_LABEL = "Actual vs. budgeted";
  private static final String COST_VALUE = "$%s / $%s";
  private static final String FORECASTED_COST_LABEL = "Forecasted vs. budgeted";
  private static final String EMPTY_VALUE = "-";
  private static final String STATUS_ON_TRACK = "On Track";
  private static final String STATUS_NOT_ON_TRACK = "Not on Track";
  private static final String STATUS_EXCEEDED = "Exceeded";

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

    return QLBudgetTrendStats.builder()
        .totalCost(getCostStats(ACTUAL_COST_LABEL, budget.getActualCost(), budget.getBudgetAmount()))
        .forecastCost(getCostStats(FORECASTED_COST_LABEL, budget.getForecastCost(), budget.getBudgetAmount()))
        .budgetDetails(budgetService.getBudgetDetails(budget))
        .status(getStatus(budget))
        .build();
  }

  private QLBillingStatsInfo getCostStats(String label, Double costValue, Double budgetedValue) {
    String statsValue = String.format(COST_VALUE, billingDataHelper.getRoundedDoubleValue(costValue),
        billingDataHelper.getRoundedDoubleValue(budgetedValue));
    return QLBillingStatsInfo.builder().statsLabel(label).statsDescription(EMPTY_VALUE).statsValue(statsValue).build();
  }

  private String getStatus(Budget budget) {
    String status = STATUS_ON_TRACK;
    if (budget.getForecastCost() > budget.getBudgetAmount()) {
      status = STATUS_NOT_ON_TRACK;
    }
    if (budget.getActualCost() > budget.getBudgetAmount()) {
      status = STATUS_EXCEEDED;
    }
    return status;
  }
}
