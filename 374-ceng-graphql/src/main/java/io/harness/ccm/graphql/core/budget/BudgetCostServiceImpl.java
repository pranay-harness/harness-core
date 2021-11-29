package io.harness.ccm.graphql.core.budget;

import static io.harness.ccm.commons.utils.BigQueryHelper.UNIFIED_TABLE;
import static io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator.AFTER;
import static io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator.BEFORE;
import static io.harness.ccm.views.graphql.QLCEViewTimeGroupType.DAY;
import static io.harness.ccm.views.graphql.QLCEViewTimeGroupType.MONTH;
import static io.harness.ccm.views.graphql.QLCEViewTimeGroupType.QUARTER;
import static io.harness.ccm.views.graphql.QLCEViewTimeGroupType.WEEK;
import static io.harness.ccm.views.graphql.QLCEViewTimeGroupType.YEAR;

import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.budget.utils.BudgetUtils;
import io.harness.ccm.commons.entities.billing.Budget;
import io.harness.ccm.commons.entities.budget.BudgetCostData;
import io.harness.ccm.commons.entities.budget.BudgetData;
import io.harness.ccm.commons.utils.BigQueryHelper;
import io.harness.ccm.graphql.core.perspectives.PerspectiveTimeSeriesHelper;
import io.harness.ccm.graphql.dto.common.TimeSeriesDataPoints;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewTimeGroupType;
import io.harness.ccm.views.graphql.QLCEViewTimeTruncGroupBy;
import io.harness.ccm.views.graphql.ViewCostData;
import io.harness.ccm.views.graphql.ViewsQueryHelper;
import io.harness.ccm.views.service.ViewsBillingService;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BudgetCostServiceImpl implements BudgetCostService {
  @Inject ViewsBillingService viewsBillingService;
  @Inject ViewsQueryHelper viewsQueryHelper;
  @Inject BigQueryService bigQueryService;
  @Inject BigQueryHelper bigQueryHelper;
  @Inject PerspectiveTimeSeriesHelper perspectiveTimeSeriesHelper;

  @Override
  public double getActualCost(Budget budget) {
    String cloudProviderTableName = bigQueryHelper.getCloudProviderTableName(budget.getAccountId(), UNIFIED_TABLE);
    String viewId = budget.getScope().getEntityIds().get(0);
    long startTime = budget.getStartTime();
    long endTime = budget.getEndTime() - BudgetUtils.ONE_DAY_MILLIS;
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(viewsQueryHelper.getViewMetadataFilter(viewId));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(startTime, AFTER));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(endTime, BEFORE));
    return getCostForPerspectiveBudget(filters, cloudProviderTableName, budget.getAccountId());
  }

  @Override
  public double getForecastCost(Budget budget) {
    String cloudProviderTableName = bigQueryHelper.getCloudProviderTableName(budget.getAccountId(), UNIFIED_TABLE);
    String perspectiveId = BudgetUtils.getPerspectiveIdForBudget(budget);
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    long startTime = BudgetUtils.getStartTimeForForecasting();
    long endTime = budget.getEndTime() - BudgetUtils.ONE_DAY_MILLIS;
    filters.add(viewsQueryHelper.getViewMetadataFilter(perspectiveId));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(startTime, AFTER));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(endTime, BEFORE));
    ViewCostData costDataForForecast =
        ViewCostData.builder()
            .cost(
                viewsBillingService
                    .getCostData(bigQueryService.get(), filters, viewsQueryHelper.getPerspectiveTotalCostAggregation(),
                        cloudProviderTableName, viewsQueryHelper.buildQueryParams(budget.getAccountId(), false))
                    .getCost())
            .minStartTime(1000 * startTime)
            .maxStartTime(1000 * BudgetUtils.getStartOfCurrentDay() - BudgetUtils.ONE_DAY_MILLIS)
            .build();
    double costTillNow = getActualCost(budget);
    return viewsQueryHelper.getRoundedDoubleValue(
        costTillNow + viewsQueryHelper.getForecastCost(costDataForForecast, Instant.ofEpochMilli(endTime)));
  }

  @Override
  public double getLastPeriodCost(Budget budget) {
    String cloudProviderTableName = bigQueryHelper.getCloudProviderTableName(budget.getAccountId(), UNIFIED_TABLE);
    String viewId = budget.getScope().getEntityIds().get(0);
    long startTime = BudgetUtils.getStartOfLastPeriod(budget.getStartTime(), budget.getPeriod());
    long endTime = budget.getStartTime() - BudgetUtils.ONE_DAY_MILLIS;
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(viewsQueryHelper.getViewMetadataFilter(viewId));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(startTime, AFTER));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(endTime, BEFORE));
    return getCostForPerspectiveBudget(filters, cloudProviderTableName, budget.getAccountId());
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
    int timeOffsetInDays = BudgetUtils.getTimeOffsetInDays(budget);
    try {
      List<TimeSeriesDataPoints> monthlyCostData = getPerspectiveBudgetTimeSeriesCostData(
          viewId, budget.getAccountId(), timeFilterValue, getTimeResolutionForBudget(budget), timeOffsetInDays);
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
                .time(data.getTime() + timeOffsetInDays * BudgetUtils.ONE_DAY_MILLIS)
                .build();
        budgetCostDataList.add(budgetCostData);
      }
    } catch (Exception e) {
      log.info("Error in generating data for budget : {}", budget.getUuid());
    }
    return BudgetData.builder().costData(budgetCostDataList).forecastCost(budget.getForecastCost()).build();
  }

  private double getCostForPerspectiveBudget(
      List<QLCEViewFilterWrapper> filters, String cloudProviderTable, String accountId) {
    ViewCostData trendData = viewsBillingService.getCostData(bigQueryService.get(), filters,
        viewsQueryHelper.getPerspectiveTotalCostAggregation(), cloudProviderTable,
        viewsQueryHelper.buildQueryParams(accountId, false));
    return trendData.getCost();
  }

  private List<TimeSeriesDataPoints> getPerspectiveBudgetTimeSeriesCostData(
      String viewId, String accountId, long startTime, QLCEViewTimeGroupType period, int timeOffsetInDays) {
    List<QLCEViewAggregation> aggregationFunction = viewsQueryHelper.getPerspectiveTotalCostAggregation();
    List<QLCEViewGroupBy> groupBy = new ArrayList<>();
    groupBy.add(QLCEViewGroupBy.builder()
                    .timeTruncGroupBy(QLCEViewTimeTruncGroupBy.builder().resolution(period).build())
                    .build());
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(viewsQueryHelper.getViewMetadataFilter(viewId));
    filters.add(viewsQueryHelper.getPerspectiveTimeFilter(startTime, AFTER));
    String cloudProviderTable = bigQueryHelper.getCloudProviderTableName(accountId, UNIFIED_TABLE);
    return perspectiveTimeSeriesHelper
        .fetch(viewsBillingService.getTimeSeriesStatsNg(bigQueryService.get(), filters, groupBy, aggregationFunction,
                   Collections.emptyList(), cloudProviderTable, true, 100,
                   viewsQueryHelper.buildQueryParams(accountId, true, false, false, false, timeOffsetInDays)),
            perspectiveTimeSeriesHelper.getTimePeriod(groupBy))
        .getStats();
  }

  private QLCEViewTimeGroupType getTimeResolutionForBudget(Budget budget) {
    try {
      switch (budget.getPeriod()) {
        case DAILY:
          return DAY;
        case WEEKLY:
          return WEEK;
        case QUARTERLY:
          return QUARTER;
        case YEARLY:
          return YEAR;
        case MONTHLY:
        default:
          return MONTH;
      }
    } catch (Exception e) {
      return MONTH;
    }
  }
}
