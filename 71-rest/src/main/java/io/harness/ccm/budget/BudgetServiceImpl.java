package io.harness.ccm.budget;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.ccm.budget.entities.AlertThreshold;
import io.harness.ccm.budget.entities.Budget;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.billing.BillingDataQueryBuilder;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.datafetcher.billing.BillingTrendStatsDataFetcher;
import software.wings.graphql.datafetcher.billing.QLBillingAmountData;
import software.wings.graphql.datafetcher.billing.QLCCMAggregationFunction;
import software.wings.graphql.datafetcher.budget.BudgetDefaultKeys;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableData;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableListData;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

@Slf4j
public class BudgetServiceImpl implements BudgetService {
  @Inject private DataFetcherUtils dataFetcherUtils;
  @Inject private BillingTrendStatsDataFetcher billingTrendStatsDataFetcher;
  @Inject private BudgetDao budgetDao;
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject private BillingDataQueryBuilder billingDataQueryBuilder;

  @Override
  public String create(Budget budget) {
    return budgetDao.save(budget);
  }

  @Override
  public void update(String budgetId, Budget budget) {
    budgetDao.update(budgetId, budget);
  }

  @Override
  public void incAlertCount(Budget budget, int thresholdIndex) {
    AlertThreshold[] alertThresholds = budget.getAlertThresholds();
    int prevAlertsSent = alertThresholds[thresholdIndex].getAlertsSent();
    alertThresholds[thresholdIndex].setAlertsSent(prevAlertsSent + 1);
    budget.setAlertThresholds(alertThresholds);
    update(budget.getUuid(), budget);
  }

  @Override
  public void setThresholdCrossedTimestamp(Budget budget, int thresholdIndex, long crossedAt) {
    AlertThreshold[] alertThresholds = budget.getAlertThresholds();
    long prevCrossedAt = alertThresholds[thresholdIndex].getCrossedAt();
    if (prevCrossedAt > budget.getCreatedAt()) {
      logger.info("The budget with id={} has already crossed the threshold #{}.", budget.getUuid(), thresholdIndex);
      return;
    }
    alertThresholds[thresholdIndex].setCrossedAt(crossedAt);
    budget.setAlertThresholds(alertThresholds);
    update(budget.getUuid(), budget);
  }

  @Override
  public Budget get(String budgetId) {
    return budgetDao.get(budgetId);
  }

  @Override
  public List<Budget> list(String accountId) {
    return budgetDao.list(accountId, 0, 0);
  }

  @Override
  public List<Budget> list(String accountId, Integer count, Integer startIndex) {
    return budgetDao.list(accountId, count, startIndex);
  }

  @Override
  public boolean delete(String budgetId) {
    return budgetDao.delete(budgetId);
  }

  @Override
  public double getActualCost(Budget budget) {
    QLBudgetTableListData data = getBudgetData(budget);
    return data.getData().get(0).getActualCost();
  }

  @Override
  public double getForecastCost(Budget budget) {
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(budget.getScope().getBudgetScopeFilter());
    QLCCMAggregationFunction aggregationFunction = BudgetUtils.makeBillingAmtAggregation();
    QLBillingAmountData billingAmountData =
        billingTrendStatsDataFetcher.getBillingAmountData(budget.getAccountId(), aggregationFunction, filters);
    Instant endInstant = billingTrendStatsDataFetcher.getEndInstant(filters);
    BigDecimal forecastCost = billingTrendStatsDataFetcher.getForecastCost(billingAmountData, endInstant);
    if (forecastCost == null) {
      return 0L;
    }
    return forecastCost.doubleValue();
  }

  @Override
  public QLBudgetTableListData getBudgetData(Budget budget) {
    Preconditions.checkNotNull(budget.getAccountId());
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(budget.getScope().getBudgetScopeFilter());
    filters.add(getFilterForCurrentBillingCycle());
    QLCCMAggregationFunction aggregationFunction = BudgetUtils.makeBillingAmtAggregation();

    QLCCMTimeSeriesAggregation groupBy = BudgetUtils.makeStartTimeEntityGroupBy();

    BillingDataQueryMetadata queryData = billingDataQueryBuilder.formBudgetInsightQuery(
        budget.getAccountId(), filters, aggregationFunction, groupBy, Collections.EMPTY_LIST);
    logger.info("BudgetDataFetcher query: {}", queryData.getQuery());

    ResultSet resultSet = null;
    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());
      Double budgetedAmount = budget.getBudgetAmount();
      return generateBudgetData(resultSet, queryData, budgetedAmount);
    } catch (SQLException e) {
      logger.error("Exception {}", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return null;
  }

  private QLBudgetTableListData generateBudgetData(
      ResultSet resultSet, BillingDataQueryMetadata queryData, Double budgetedAmount) throws SQLException {
    List<QLBudgetTableData> budgetTableDataList = new ArrayList<>();

    Double actualCost = BudgetDefaultKeys.ACTUAL_COST;
    long time = BudgetDefaultKeys.TIME;

    while (resultSet != null && resultSet.next()) {
      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field) {
          case SUM:
            actualCost = resultSet.getDouble(field.getFieldName());
            break;
          case TIME_SERIES:
            time = resultSet.getTimestamp(field.getFieldName(), dataFetcherUtils.getDefaultCalendar()).getTime();
            break;
          default:
            break;
        }
      }

      Double budgetVariance = getBudgetVariance(budgetedAmount, actualCost);

      Double budgetVariancePercentage = getBudgetVariancePercentage(budgetVariance, budgetedAmount);

      QLBudgetTableData qlBudgetTableData = QLBudgetTableData.builder()
                                                .actualCost(actualCost)
                                                .budgeted(budgetedAmount)
                                                .budgetVariance(budgetVariance)
                                                .budgetVariancePercentage(budgetVariancePercentage)
                                                .time(time)
                                                .build();
      budgetTableDataList.add(qlBudgetTableData);
    }
    return QLBudgetTableListData.builder().data(budgetTableDataList).build();
  }

  private static Double getBudgetVariance(Double budgetedAmount, Double actualCost) {
    return budgetedAmount - actualCost;
  }

  private static Double getBudgetVariancePercentage(Double budgetVariance, Double budgetedAmount) {
    Double budgetVariancePercentage;
    if (budgetedAmount != 0) {
      budgetVariancePercentage = (budgetVariance / budgetedAmount) * 100;
    } else {
      budgetVariancePercentage = 0.0;
    }
    return budgetVariancePercentage;
  }

  private QLBillingDataFilter getFilterForCurrentBillingCycle() {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.DAY_OF_MONTH, 1);
    long startTime = c.getTimeInMillis();
    return QLBillingDataFilter.builder()
        .startTime(QLTimeFilter.builder().operator(QLTimeOperator.AFTER).value(startTime).build())
        .build();
  }
}
