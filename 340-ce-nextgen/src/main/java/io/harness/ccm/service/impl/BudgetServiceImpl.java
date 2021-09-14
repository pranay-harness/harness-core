/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.service.impl;

import static io.harness.ccm.commons.utils.BigQueryHelper.UNIFIED_TABLE;
import static io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator.AFTER;
import static io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator.BEFORE;

import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.budget.AlertThreshold;
import io.harness.ccm.budget.BudgetScope;
import io.harness.ccm.budget.dao.BudgetDao;
import io.harness.ccm.budget.utils.BudgetUtils;
import io.harness.ccm.commons.entities.billing.Budget;
import io.harness.ccm.commons.entities.budget.BudgetCostData;
import io.harness.ccm.commons.entities.budget.BudgetData;
import io.harness.ccm.commons.utils.BigQueryHelper;
import io.harness.ccm.graphql.core.perspectives.PerspectiveTimeSeriesHelper;
import io.harness.ccm.graphql.dto.common.TimeSeriesDataPoints;
import io.harness.ccm.service.intf.BudgetService;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewTimeGroupType;
import io.harness.ccm.views.graphql.QLCEViewTimeTruncGroupBy;
import io.harness.ccm.views.graphql.ViewCostData;
import io.harness.ccm.views.graphql.ViewsQueryHelper;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ccm.views.service.ViewsBillingService;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
public class BudgetServiceImpl implements BudgetService {
  @Inject private BudgetDao budgetDao;
  @Inject private CEViewService ceViewService;
  @Inject ViewsBillingService viewsBillingService;
  @Inject ViewsQueryHelper viewsQueryHelper;
  @Inject PerspectiveTimeSeriesHelper perspectiveTimeSeriesHelper;
  @Inject BigQueryService bigQueryService;
  @Inject BigQueryHelper bigQueryHelper;

  @Override
  public String create(Budget budget) {
    BudgetUtils.validateBudget(budget, budgetDao.list(budget.getAccountId(), budget.getName()));
    removeEmailDuplicates(budget);
    validatePerspective(budget);
    updateBudgetCosts(budget);
    return budgetDao.save(budget);
  }

  @Override
  public String clone(String budgetId, String cloneBudgetName, String accountId) {
    Budget budget = budgetDao.get(budgetId, accountId);
    BudgetUtils.validateBudget(budget, budgetDao.list(budget.getAccountId(), budget.getName()));
    BudgetUtils.validateCloneBudgetName(cloneBudgetName);
    Budget cloneBudget = Budget.builder()
                             .accountId(budget.getAccountId())
                             .name(cloneBudgetName)
                             .scope(budget.getScope())
                             .type(budget.getType())
                             .budgetAmount(budget.getBudgetAmount())
                             .actualCost(budget.getActualCost())
                             .forecastCost(budget.getForecastCost())
                             .lastMonthCost(budget.getLastMonthCost())
                             .alertThresholds(budget.getAlertThresholds())
                             .userGroupIds(budget.getUserGroupIds())
                             .emailAddresses(budget.getEmailAddresses())
                             .notifyOnSlack(budget.isNotifyOnSlack())
                             .build();
    return create(cloneBudget);
  }

  @Override
  public Budget get(String budgetId, String accountId) {
    return budgetDao.get(budgetId, accountId);
  }

  @Override
  public void update(String budgetId, Budget budget) {
    if (budget.getAccountId() == null) {
      Budget existingBudget = budgetDao.get(budgetId);
      budget.setAccountId(existingBudget.getAccountId());
    }
    if (budget.getUuid() == null) {
      budget.setUuid(budgetId);
    }
    BudgetUtils.validateBudget(budget, budgetDao.list(budget.getAccountId(), budget.getName()));
    removeEmailDuplicates(budget);
    validatePerspective(budget);
    updateBudgetCosts(budget);
    budgetDao.update(budgetId, budget);
  }

  @Override
  public List<Budget> list(String accountId) {
    return budgetDao.list(accountId, Integer.MAX_VALUE - 1, 0);
  }

  @Override
  public List<Budget> list(String accountId, String perspectiveId) {
    List<Budget> budgets = budgetDao.list(accountId, Integer.MAX_VALUE - 1, 0);
    return budgets.stream()
        .filter(budget -> isBudgetBasedOnGivenPerspective(budget, perspectiveId))
        .collect(Collectors.toList());
  }

  @Override
  public boolean delete(String budgetId, String accountId) {
    return budgetDao.delete(budgetId, accountId);
  }

  @Override
  public Double getLastMonthCostForPerspective(String accountId, String perspectiveId) {
    if (ceViewService.get(perspectiveId) == null) {
      throw new InvalidRequestException(BudgetUtils.INVALID_PERSPECTIVE_ID_EXCEPTION);
    }
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(viewsQueryHelper.getViewMetadataFilter(perspectiveId));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(BudgetUtils.getStartOfMonth(true), AFTER));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(
        BudgetUtils.getStartOfMonth(false) - BudgetUtils.ONE_DAY_MILLIS, BEFORE));
    return getCostForPerspective(accountId, filters);
  }

  @Override
  public Double getForecastCostForPerspective(String accountId, String perspectiveId) {
    if (ceViewService.get(perspectiveId) == null) {
      throw new InvalidRequestException(BudgetUtils.INVALID_PERSPECTIVE_ID_EXCEPTION);
    }
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    long startTime = BudgetUtils.getStartTimeForForecasting();
    long endTime = BudgetUtils.getEndOfMonthForCurrentBillingCycle();
    filters.add(viewsQueryHelper.getViewMetadataFilter(perspectiveId));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(startTime, AFTER));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(endTime, BEFORE));
    String cloudProviderTable = bigQueryHelper.getCloudProviderTableName(accountId, UNIFIED_TABLE);
    ViewCostData costDataForForecast =
        ViewCostData.builder()
            .cost(
                viewsBillingService
                    .getCostData(bigQueryService.get(), filters, viewsQueryHelper.getPerspectiveTotalCostAggregation(),
                        cloudProviderTable, viewsQueryHelper.buildQueryParams(accountId, false))
                    .getCost())
            .minStartTime(1000 * startTime)
            .maxStartTime(1000 * BudgetUtils.getStartOfCurrentDay() - BudgetUtils.ONE_DAY_MILLIS)
            .build();
    double costTillNow = getActualCostForPerspectiveBudget(accountId, perspectiveId);
    return viewsQueryHelper.getRoundedDoubleValue(
        costTillNow + viewsQueryHelper.getForecastCost(costDataForForecast, Instant.ofEpochMilli(endTime)));
  }

  @Override
  public BudgetData getBudgetTimeSeriesStats(Budget budget) {
    if (budget == null) {
      throw new InvalidRequestException(BudgetUtils.INVALID_BUDGET_ID_EXCEPTION);
    }
    List<BudgetCostData> budgetCostDataList = new ArrayList<>();
    Double budgetedAmount = budget.getBudgetAmount();
    if (budgetedAmount == null) {
      budgetedAmount = 0.0;
    }

    String viewId = budget.getScope().getEntityIds().get(0);
    long timeFilterValue = BudgetUtils.getStartTimeForCurrentBillingCycle();
    try {
      List<TimeSeriesDataPoints> monthlyCostData =
          getPerspectiveBudgetMonthlyCostData(viewId, budget.getAccountId(), timeFilterValue);

      for (TimeSeriesDataPoints data : monthlyCostData) {
        double actualCost =
            data.getValues().stream().map(dataPoint -> dataPoint.getValue().doubleValue()).reduce(0D, Double::sum);
        double budgetVariance = BudgetUtils.getBudgetVariance(budgetedAmount, actualCost);
        double budgetVariancePercentage = BudgetUtils.getBudgetVariancePercentage(budgetVariance, budgetedAmount);
        BudgetCostData budgetCostData =
            BudgetCostData.builder()
                .actualCost(viewsQueryHelper.getRoundedDoubleValue(actualCost))
                .budgeted(viewsQueryHelper.getRoundedDoubleValue(budgetedAmount))
                .budgetVariance(viewsQueryHelper.getRoundedDoubleValue(budgetVariance))
                .budgetVariancePercentage(viewsQueryHelper.getRoundedDoubleValue(budgetVariancePercentage))
                .time(data.getTime())
                .build();
        budgetCostDataList.add(budgetCostData);
      }
    } catch (Exception e) {
      log.info("Error in generating data for budget : {}", budget.getUuid());
    }
    return BudgetData.builder().costData(budgetCostDataList).forecastCost(budget.getForecastCost()).build();
  }

  private void validatePerspective(Budget budget) {
    BudgetScope scope = budget.getScope();
    String[] entityIds = BudgetUtils.getAppliesToIds(scope);
    log.debug("entityIds is {}", entityIds);
    if (ceViewService.get(entityIds[0]) == null) {
      throw new InvalidRequestException(BudgetUtils.INVALID_ENTITY_ID_EXCEPTION);
    }
  }

  public boolean isBudgetBasedOnGivenPerspective(Budget budget, String perspectiveId) {
    return budget.getScope().getEntityIds().get(0).equals(perspectiveId);
  }

  private void removeEmailDuplicates(Budget budget) {
    String[] emailAddresses = ArrayUtils.nullToEmpty(budget.getEmailAddresses());
    String[] uniqueEmailAddresses = new HashSet<>(Arrays.asList(emailAddresses)).toArray(new String[0]);
    budget.setEmailAddresses(uniqueEmailAddresses);
    // In NG we have per alertThreshold separate email addresses
    AlertThreshold[] alertThresholds = budget.getAlertThresholds();
    if (alertThresholds != null && alertThresholds.length > 0) {
      for (AlertThreshold alertThreshold : alertThresholds) {
        emailAddresses = ArrayUtils.nullToEmpty(alertThreshold.getEmailAddresses());
        uniqueEmailAddresses = new HashSet<>(Arrays.asList(emailAddresses)).toArray(new String[0]);
        alertThreshold.setEmailAddresses(uniqueEmailAddresses);
      }
      budget.setAlertThresholds(alertThresholds);
    }
  }

  // Methods for updating costs for budget
  private void updateBudgetCosts(Budget budget) {
    try {
      Double actualCost = getActualCostForPerspectiveBudget(budget);
      Double forecastCost = actualCost + getForecastCostForPerspectiveBudget(budget);
      Double lastMonthCost = getLastMonthCostForPerspectiveBudget(budget);

      budget.setActualCost(actualCost);
      budget.setForecastCost(forecastCost);
      budget.setLastMonthCost(lastMonthCost);
    } catch (Exception e) {
      log.error("Error occurred while updating costs of budget: {}, Exception : {}", budget.getUuid(), e);
    }
  }

  private double getActualCostForPerspectiveBudget(String accountId, String perspectiveId) {
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(viewsQueryHelper.getViewMetadataFilter(perspectiveId));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(BudgetUtils.getStartOfMonthForCurrentBillingCycle(), AFTER));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(BudgetUtils.getEndOfMonthForCurrentBillingCycle(), BEFORE));
    return getCostForPerspective(accountId, filters);
  }

  private double getActualCostForPerspectiveBudget(Budget budget) {
    return getActualCostForPerspectiveBudget(budget.getAccountId(), budget.getScope().getEntityIds().get(0));
  }

  private double getLastMonthCostForPerspectiveBudget(Budget budget) {
    return getLastMonthCostForPerspective(budget.getAccountId(), budget.getScope().getEntityIds().get(0));
  }

  private double getForecastCostForPerspectiveBudget(Budget budget) {
    return getForecastCostForPerspective(budget.getAccountId(), budget.getScope().getEntityIds().get(0));
  }

  private double getCostForPerspective(String accountId, List<QLCEViewFilterWrapper> filters) {
    String cloudProviderTable = bigQueryHelper.getCloudProviderTableName(accountId, UNIFIED_TABLE);
    ViewCostData costData = viewsBillingService.getCostData(bigQueryService.get(), filters,
        viewsQueryHelper.getPerspectiveTotalCostAggregation(), cloudProviderTable,
        viewsQueryHelper.buildQueryParams(accountId, false));
    return costData.getCost();
  }

  private List<TimeSeriesDataPoints> getPerspectiveBudgetMonthlyCostData(
      String viewId, String accountId, long startTime) {
    List<QLCEViewAggregation> aggregationFunction = viewsQueryHelper.getPerspectiveTotalCostAggregation();
    List<QLCEViewGroupBy> groupBy = new ArrayList<>();
    groupBy.add(
        QLCEViewGroupBy.builder()
            .timeTruncGroupBy(QLCEViewTimeTruncGroupBy.builder().resolution(QLCEViewTimeGroupType.MONTH).build())
            .build());
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(viewsQueryHelper.getViewMetadataFilter(viewId));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(startTime, AFTER));
    String cloudProviderTable = bigQueryHelper.getCloudProviderTableName(accountId, UNIFIED_TABLE);
    return perspectiveTimeSeriesHelper
        .fetch(viewsBillingService.getTimeSeriesStats(bigQueryService.get(), filters, groupBy, aggregationFunction,
                   Collections.emptyList(), cloudProviderTable),
            perspectiveTimeSeriesHelper.getTimePeriod(groupBy))
        .getStats();
  }
}
